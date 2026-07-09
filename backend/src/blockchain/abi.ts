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
  // Enumerasi semua token (explore/marketplace) + approval (list ke marketplace)
  'function totalSupply() view returns (uint256)',
  'function tokenByIndex(uint256 index) view returns (uint256)',
  'function approve(address to, uint256 tokenId)',
  'function getApproved(uint256 tokenId) view returns (address)',
  'function isApprovedForAll(address owner, address operator) view returns (bool)',
  'event PassportMinted(uint256 indexed tokenId, address indexed owner, uint8 authenticityScore)',
  'event PassportTransferred(uint256 indexed tokenId, address indexed from, address indexed to)',
  // Untuk queryFilter recall & service record (fungsi ini hanya emit event)
  'event ServiceRecordAdded(uint256 indexed tokenId, string recordObjectId)',
  'event RecallIssued(string brand, string category, string reason)',
  'function addServiceRecord(uint256 tokenId, string recordObjectId)',
  'function issueRecall(string brand, string category, string reason)',
];

/** ABI OriginTagMarketplace (escrow). */
export const ORIGINTAG_MARKETPLACE_ABI = [
  'function listItem(uint256 tokenId, uint256 price)',
  'function cancelListing(uint256 tokenId)',
  'function buyItem(uint256 tokenId) payable',
  'function getListing(uint256 tokenId) view returns (tuple(address seller, uint256 price, bool active))',
  'event ListingCreated(uint256 indexed tokenId, address indexed seller, uint256 price)',
  'event ListingCancelled(uint256 indexed tokenId, address indexed seller)',
  'event ItemSold(uint256 indexed tokenId, address indexed seller, address indexed buyer, uint256 price)',
];
