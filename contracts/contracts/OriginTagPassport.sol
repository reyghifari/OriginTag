// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {ERC721} from "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import {ERC721Enumerable} from "@openzeppelin/contracts/token/ERC721/extensions/ERC721Enumerable.sol";
import {AccessControl} from "@openzeppelin/contracts/access/AccessControl.sol";
import {ReentrancyGuard} from "@openzeppelin/contracts/utils/ReentrancyGuard.sol";

/// @title OriginTagPassport — digital passport BEP-721 untuk barang fisik (PRD §10)
/// @notice Setiap tokenId merepresentasikan satu unit barang fisik. Bukti otentikasi
///         (foto, hasil AI) disimpan di BNB Greenfield dan direferensikan lewat
///         `evidenceObjectId` (FR-05).
contract OriginTagPassport is ERC721Enumerable, AccessControl, ReentrancyGuard {
    bytes32 public constant AUTHENTICATOR_ROLE = keccak256("AUTHENTICATOR_ROLE");
    bytes32 public constant SERVICE_ROLE = keccak256("SERVICE_ROLE");

    struct Passport {
        string brand;
        string category;
        string serialNumber;
        uint256 originalPurchaseDate;
        uint256 warrantyDurationSeconds;
        uint8 authenticityScore; // 0-100 dari AI Authentication Engine
        string evidenceObjectId; // reference ke object di BNB Greenfield
        bool isFlaggedForReview;
    }

    // Riwayat kepemilikan dipisah dari struct supaya bisa dikembalikan utuh
    // lewat getter (getter otomatis mapping public tidak mengembalikan array dalam struct).
    mapping(uint256 tokenId => Passport) private _passports;
    mapping(uint256 tokenId => address[]) private _ownershipHistory;
    uint256 private _nextTokenId;

    event PassportMinted(uint256 indexed tokenId, address indexed owner, uint8 authenticityScore);
    event PassportTransferred(uint256 indexed tokenId, address indexed from, address indexed to);
    event ServiceRecordAdded(uint256 indexed tokenId, string recordObjectId);
    event RecallIssued(string brand, string category, string reason);

    constructor() ERC721("OriginTag Passport", "OTAG") {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
    }

    /// @notice Mint passport baru. Hanya backend OriginTag (AUTHENTICATOR_ROLE)
    ///         setelah AI verification lolos threshold (PRD §10.2).
    function mintPassport(
        address to,
        string calldata brand,
        string calldata category,
        string calldata serialNumber,
        uint256 warrantyDurationSeconds,
        uint8 authenticityScore,
        string calldata evidenceObjectId
    ) external onlyRole(AUTHENTICATOR_ROLE) returns (uint256) {
        require(authenticityScore <= 100, "OriginTag: skor harus 0-100");

        uint256 tokenId = _nextTokenId++;
        _safeMint(to, tokenId);

        Passport storage p = _passports[tokenId];
        p.brand = brand;
        p.category = category;
        p.serialNumber = serialNumber;
        p.originalPurchaseDate = block.timestamp;
        p.warrantyDurationSeconds = warrantyDurationSeconds;
        p.authenticityScore = authenticityScore;
        p.evidenceObjectId = evidenceObjectId;
        // ownershipHistory dicatat otomatis di _update() saat _safeMint di atas.

        emit PassportMinted(tokenId, to, authenticityScore);
        return tokenId;
    }

    /// @notice Transfer passport P2P langsung saat barang terjual (FR-07). Validasi
    ///         kepemilikan on-chain (PRD §8.1). ownershipHistory dicatat di _update().
    ///         Transfer lewat marketplace (safeTransferFrom) juga terekam via _update().
    function transferPassport(uint256 tokenId, address to) external nonReentrant {
        require(ownerOf(tokenId) == msg.sender, "OriginTag: bukan pemilik passport");
        _safeTransfer(msg.sender, to, tokenId, "");
        emit PassportTransferred(tokenId, msg.sender, to);
    }

    /// @notice Sisa masa garansi dalam detik, dihitung dari tanggal beli asli (FR-08).
    ///         Nilai tidak di-reset saat transfer — garansi mengikuti barang, bukan pemilik.
    function getRemainingWarranty(uint256 tokenId) external view returns (uint256) {
        _requireOwned(tokenId);
        Passport storage p = _passports[tokenId];
        uint256 elapsed = block.timestamp - p.originalPurchaseDate;
        if (elapsed >= p.warrantyDurationSeconds) return 0;
        return p.warrantyDurationSeconds - elapsed;
    }

    function getPassport(uint256 tokenId) external view returns (Passport memory) {
        _requireOwned(tokenId);
        return _passports[tokenId];
    }

    function getOwnershipHistory(uint256 tokenId) external view returns (address[] memory) {
        _requireOwned(tokenId);
        return _ownershipHistory[tokenId];
    }

    /// @notice Fase 2 (FR-10): service center terverifikasi mencatat riwayat servis.
    function addServiceRecord(uint256 tokenId, string calldata recordObjectId)
        external
        onlyRole(SERVICE_ROLE)
    {
        _requireOwned(tokenId);
        emit ServiceRecordAdded(tokenId, recordObjectId);
    }

    /// @notice Fase 2 (FR-11): recall per brand+kategori, didengarkan backend indexer.
    function issueRecall(string calldata brand, string calldata category, string calldata reason)
        external
        onlyRole(AUTHENTICATOR_ROLE)
    {
        emit RecallIssued(brand, category, reason);
    }

    /// @dev Hook transfer OZ v5 — dipanggil di SEMUA jalur (mint, transferPassport,
    ///      dan safeTransferFrom marketplace). Di sinilah ownershipHistory dicatat,
    ///      sehingga penjualan lewat marketplace pun terekam riwayatnya.
    function _update(address to, uint256 tokenId, address auth)
        internal
        override(ERC721Enumerable)
        returns (address)
    {
        address from = super._update(to, tokenId, auth);
        if (to != address(0)) {
            _ownershipHistory[tokenId].push(to);
        }
        return from;
    }

    function _increaseBalance(address account, uint128 value)
        internal
        override(ERC721Enumerable)
    {
        super._increaseBalance(account, value);
    }

    function supportsInterface(bytes4 interfaceId)
        public
        view
        override(ERC721Enumerable, AccessControl)
        returns (bool)
    {
        return super.supportsInterface(interfaceId);
    }
}
