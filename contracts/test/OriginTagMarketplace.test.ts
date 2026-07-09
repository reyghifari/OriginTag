import { expect } from "chai";
import { ethers } from "hardhat";
import { loadFixture } from "@nomicfoundation/hardhat-toolbox/network-helpers";

const WARRANTY_1_YEAR = 365 * 24 * 60 * 60;
const PRICE = ethers.parseEther("1"); // 1 tBNB
const FEE_BPS = 200n; // 2%

describe("OriginTagMarketplace", () => {
  async function deployFixture() {
    const [admin, authenticator, seller, buyer, feeRecipient] = await ethers.getSigners();

    const passportFactory = await ethers.getContractFactory("OriginTagPassport");
    const passport = (await passportFactory.deploy()) as any;
    await passport.grantRole(await passport.AUTHENTICATOR_ROLE(), authenticator.address);

    const marketFactory = await ethers.getContractFactory("OriginTagMarketplace");
    const market = (await marketFactory.deploy(
      await passport.getAddress(),
      feeRecipient.address,
      FEE_BPS,
    )) as any;

    // Mint passport ke seller (tokenId 0)
    await passport
      .connect(authenticator)
      .mintPassport(seller.address, "Nike", "Sneakers", "SN-1", WARRANTY_1_YEAR, 95, "greenfield://x/0");

    return { passport, market, admin, authenticator, seller, buyer, feeRecipient };
  }

  it("seller bisa list setelah approve", async () => {
    const { passport, market, seller } = await loadFixture(deployFixture);
    await passport.connect(seller).approve(await market.getAddress(), 0);

    await expect(market.connect(seller).listItem(0, PRICE))
      .to.emit(market, "ListingCreated")
      .withArgs(0, seller.address, PRICE);

    const listing = await market.getListing(0);
    expect(listing.seller).to.equal(seller.address);
    expect(listing.price).to.equal(PRICE);
    expect(listing.active).to.equal(true);
  });

  it("tidak bisa list tanpa approve", async () => {
    const { market, seller } = await loadFixture(deployFixture);
    await expect(market.connect(seller).listItem(0, PRICE)).to.be.revertedWith(
      "Marketplace: belum di-approve",
    );
  });

  it("non-pemilik tidak bisa list", async () => {
    const { market, buyer } = await loadFixture(deployFixture);
    await expect(market.connect(buyer).listItem(0, PRICE)).to.be.revertedWith(
      "Marketplace: bukan pemilik",
    );
  });

  it("buyItem: NFT pindah, seller dibayar, fee ke recipient, riwayat terekam", async () => {
    const { passport, market, seller, buyer, feeRecipient } = await loadFixture(deployFixture);
    await passport.connect(seller).approve(await market.getAddress(), 0);
    await market.connect(seller).listItem(0, PRICE);

    const sellerBefore = await ethers.provider.getBalance(seller.address);
    const feeBefore = await ethers.provider.getBalance(feeRecipient.address);

    await expect(market.connect(buyer).buyItem(0, { value: PRICE }))
      .to.emit(market, "ItemSold")
      .withArgs(0, seller.address, buyer.address, PRICE);

    // NFT pindah ke buyer
    expect(await passport.ownerOf(0)).to.equal(buyer.address);

    // Seller terima harga - fee; feeRecipient terima fee
    const fee = (PRICE * FEE_BPS) / 10_000n;
    expect(await ethers.provider.getBalance(seller.address)).to.equal(sellerBefore + (PRICE - fee));
    expect(await ethers.provider.getBalance(feeRecipient.address)).to.equal(feeBefore + fee);

    // Riwayat kepemilikan mencakup buyer (bukti refactor _update menangkap transfer marketplace)
    const history = await passport.getOwnershipHistory(0);
    expect(history).to.deep.equal([seller.address, buyer.address]);

    // Listing terhapus
    expect((await market.getListing(0)).active).to.equal(false);
  });

  it("refund kelebihan bayar", async () => {
    const { passport, market, seller, buyer } = await loadFixture(deployFixture);
    await passport.connect(seller).approve(await market.getAddress(), 0);
    await market.connect(seller).listItem(0, PRICE);

    const overpay = ethers.parseEther("1.5");
    const buyerBefore = await ethers.provider.getBalance(buyer.address);
    const tx = await market.connect(buyer).buyItem(0, { value: overpay });
    const receipt = await tx.wait();
    const gas = receipt!.gasUsed * receipt!.gasPrice;

    // Buyer hanya kehilangan PRICE + gas (kelebihan 0.5 di-refund)
    expect(await ethers.provider.getBalance(buyer.address)).to.equal(buyerBefore - PRICE - gas);
  });

  it("garansi tetap terhitung dari originalPurchaseDate setelah dibeli (FR-08)", async () => {
    const { passport, market, seller, buyer } = await loadFixture(deployFixture);
    await passport.connect(seller).approve(await market.getAddress(), 0);
    await market.connect(seller).listItem(0, PRICE);
    await market.connect(buyer).buyItem(0, { value: PRICE });

    // Garansi masih hampir setahun (tidak reset saat jual-beli)
    const remaining = await passport.getRemainingWarranty(0);
    expect(remaining).to.be.closeTo(BigInt(WARRANTY_1_YEAR), 60n);
  });

  it("revert: beli item yang tidak dijual", async () => {
    const { market, buyer } = await loadFixture(deployFixture);
    await expect(market.connect(buyer).buyItem(0, { value: PRICE })).to.be.revertedWith(
      "Marketplace: tidak dijual",
    );
  });

  it("revert: dana kurang", async () => {
    const { passport, market, seller, buyer } = await loadFixture(deployFixture);
    await passport.connect(seller).approve(await market.getAddress(), 0);
    await market.connect(seller).listItem(0, PRICE);
    await expect(
      market.connect(buyer).buyItem(0, { value: ethers.parseEther("0.5") }),
    ).to.be.revertedWith("Marketplace: dana kurang");
  });

  it("seller bisa cancel listing", async () => {
    const { passport, market, seller } = await loadFixture(deployFixture);
    await passport.connect(seller).approve(await market.getAddress(), 0);
    await market.connect(seller).listItem(0, PRICE);

    await expect(market.connect(seller).cancelListing(0))
      .to.emit(market, "ListingCancelled")
      .withArgs(0, seller.address);
    expect((await market.getListing(0)).active).to.equal(false);
  });
});
