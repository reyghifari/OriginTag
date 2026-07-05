/**
 * ABI minimal OriginTagPassport (human-readable, format ethers v6).
 * Sinkronkan dengan contracts/artifacts setelah kontrak berubah & compile ulang.
 */
export const ORIGINTAG_PASSPORT_ABI = [
  'function mintPassport(address to, string brand, string category, string serialNumber, uint256 warrantyDurationSeconds, uint8 authenticityScore, string evidenceObjectId) returns (uint256)',
  'function transferPassport(uint256 tokenId, address to)',
  'function getRemainingWarranty(uint256 tokenId) view returns (uint256)',
  'function getPassport(uint256 tokenId) view returns (tuple(string brand, string category, string serialNumber, uint256 originalPurchaseDate, uint256 warrantyDurationSeconds, uint8 authenticityScore, string evidenceObjectId, bool isFlaggedForReview))',
  'function getOwnershipHistory(uint256 tokenId) view returns (address[])',
  'function ownerOf(uint256 tokenId) view returns (address)',
  'function balanceOf(address owner) view returns (uint256)',
  'function tokenOfOwnerByIndex(address owner, uint256 index) view returns (uint256)',
  'event PassportMinted(uint256 indexed tokenId, address indexed owner, uint8 authenticityScore)',
  'event PassportTransferred(uint256 indexed tokenId, address indexed from, address indexed to)',
];
