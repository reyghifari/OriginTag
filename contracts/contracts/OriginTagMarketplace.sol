// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {IERC721} from "@openzeppelin/contracts/token/ERC721/IERC721.sol";
import {ReentrancyGuard} from "@openzeppelin/contracts/utils/ReentrancyGuard.sol";
import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";

/// @title OriginTagMarketplace — reference marketplace escrow untuk Passport NFT.
/// @notice Membuktikan passport OriginTag bisa dijualbelikan lintas platform;
///         kepemilikan + garansi (via originalPurchaseDate immutable di passport)
///         otomatis ikut berpindah ke pembeli. Pembayaran dalam tBNB (native).
contract OriginTagMarketplace is ReentrancyGuard, Ownable {
    IERC721 public immutable passport;

    /// Fee platform dalam basis points (200 = 2%). Maks 10%.
    uint96 public feeBps;
    address public feeRecipient;

    struct Listing {
        address seller;
        uint256 price;
        bool active;
    }

    mapping(uint256 tokenId => Listing) private _listings;

    event ListingCreated(uint256 indexed tokenId, address indexed seller, uint256 price);
    event ListingCancelled(uint256 indexed tokenId, address indexed seller);
    event ItemSold(uint256 indexed tokenId, address indexed seller, address indexed buyer, uint256 price);
    event FeeUpdated(uint96 feeBps, address feeRecipient);

    constructor(address passportAddress, address feeRecipient_, uint96 feeBps_) Ownable(msg.sender) {
        require(passportAddress != address(0), "Marketplace: passport nol");
        require(feeBps_ <= 1000, "Marketplace: fee maks 10%");
        passport = IERC721(passportAddress);
        feeRecipient = feeRecipient_;
        feeBps = feeBps_;
    }

    /// @notice Jual passport pada harga tertentu. Seller wajib approve marketplace dulu
    ///         (approve(tokenId) atau setApprovalForAll).
    function listItem(uint256 tokenId, uint256 price) external {
        require(price > 0, "Marketplace: harga harus > 0");
        require(passport.ownerOf(tokenId) == msg.sender, "Marketplace: bukan pemilik");
        require(
            passport.getApproved(tokenId) == address(this) ||
                passport.isApprovedForAll(msg.sender, address(this)),
            "Marketplace: belum di-approve"
        );
        _listings[tokenId] = Listing({seller: msg.sender, price: price, active: true});
        emit ListingCreated(tokenId, msg.sender, price);
    }

    /// @notice Batalkan listing (hanya seller).
    function cancelListing(uint256 tokenId) external {
        Listing memory l = _listings[tokenId];
        require(l.active && l.seller == msg.sender, "Marketplace: tidak bisa dibatalkan");
        delete _listings[tokenId];
        emit ListingCancelled(tokenId, msg.sender);
    }

    /// @notice Beli passport. Bayar >= harga; NFT pindah ke pembeli, dana ke seller
    ///         (dikurangi fee), kelebihan di-refund.
    function buyItem(uint256 tokenId) external payable nonReentrant {
        Listing memory l = _listings[tokenId];
        require(l.active, "Marketplace: tidak dijual");
        require(msg.value >= l.price, "Marketplace: dana kurang");
        require(passport.ownerOf(tokenId) == l.seller, "Marketplace: penjual bukan pemilik lagi");
        require(msg.sender != l.seller, "Marketplace: tidak bisa beli sendiri");

        delete _listings[tokenId];

        uint256 fee = (l.price * feeBps) / 10_000;
        uint256 sellerAmount = l.price - fee;

        // Pindahkan NFT dari seller ke pembeli (memicu _update di passport → catat riwayat).
        passport.safeTransferFrom(l.seller, msg.sender, tokenId);

        // Bayar seller.
        (bool paidSeller, ) = payable(l.seller).call{value: sellerAmount}("");
        require(paidSeller, "Marketplace: bayar seller gagal");

        // Bayar fee platform (jika ada).
        if (fee > 0) {
            (bool paidFee, ) = payable(feeRecipient).call{value: fee}("");
            require(paidFee, "Marketplace: bayar fee gagal");
        }

        // Refund kelebihan bayar.
        uint256 excess = msg.value - l.price;
        if (excess > 0) {
            (bool refunded, ) = payable(msg.sender).call{value: excess}("");
            require(refunded, "Marketplace: refund gagal");
        }

        emit ItemSold(tokenId, l.seller, msg.sender, l.price);
    }

    function getListing(uint256 tokenId) external view returns (Listing memory) {
        return _listings[tokenId];
    }

    /// @notice Admin: atur fee platform.
    function setFee(uint96 feeBps_, address feeRecipient_) external onlyOwner {
        require(feeBps_ <= 1000, "Marketplace: fee maks 10%");
        feeBps = feeBps_;
        feeRecipient = feeRecipient_;
        emit FeeUpdated(feeBps_, feeRecipient_);
    }
}
