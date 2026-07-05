import { expect } from "chai";
import { ethers } from "hardhat";
import { loadFixture, time } from "@nomicfoundation/hardhat-toolbox/network-helpers";

const WARRANTY_1_YEAR = 365 * 24 * 60 * 60;

describe("OriginTagPassport", () => {
  async function deployFixture() {
    const [admin, authenticator, seller, buyer] = await ethers.getSigners();
    const factory = await ethers.getContractFactory("OriginTagPassport");
    const passport = (await factory.deploy()) as any;
    await passport.grantRole(await passport.AUTHENTICATOR_ROLE(), authenticator.address);
    return { passport, admin, authenticator, seller, buyer };
  }

  function mint(passport: any, authenticator: any, to: string) {
    return passport
      .connect(authenticator)
      .mintPassport(to, "Nike", "Sneakers", "SN-001", WARRANTY_1_YEAR, 92, "greenfield://origintag-evidence/0");
  }

  it("authenticator dapat mint passport dengan data lengkap", async () => {
    const { passport, authenticator, seller } = await loadFixture(deployFixture);

    await expect(mint(passport, authenticator, seller.address))
      .to.emit(passport, "PassportMinted")
      .withArgs(0, seller.address, 92);

    expect(await passport.ownerOf(0)).to.equal(seller.address);
    const p = await passport.getPassport(0);
    expect(p.brand).to.equal("Nike");
    expect(p.authenticityScore).to.equal(92);
  });

  it("wallet tanpa AUTHENTICATOR_ROLE tidak bisa mint", async () => {
    const { passport, seller } = await loadFixture(deployFixture);
    await expect(mint(passport, seller, seller.address)).to.be.reverted;
  });

  it("transferPassport memindahkan NFT dan mencatat riwayat kepemilikan", async () => {
    const { passport, authenticator, seller, buyer } = await loadFixture(deployFixture);
    await mint(passport, authenticator, seller.address);

    await expect(passport.connect(seller).transferPassport(0, buyer.address))
      .to.emit(passport, "PassportTransferred")
      .withArgs(0, seller.address, buyer.address);

    expect(await passport.ownerOf(0)).to.equal(buyer.address);
    const history = await passport.getOwnershipHistory(0);
    expect(history).to.deep.equal([seller.address, buyer.address]);
  });

  it("non-pemilik tidak bisa transfer passport orang lain", async () => {
    const { passport, authenticator, seller, buyer } = await loadFixture(deployFixture);
    await mint(passport, authenticator, seller.address);

    await expect(
      passport.connect(buyer).transferPassport(0, buyer.address),
    ).to.be.revertedWith("OriginTag: bukan pemilik passport");
  });

  it("sisa garansi berkurang seiring waktu dan tidak reset saat transfer (FR-08)", async () => {
    const { passport, authenticator, seller, buyer } = await loadFixture(deployFixture);
    await mint(passport, authenticator, seller.address);

    await time.increase(100 * 24 * 60 * 60); // maju 100 hari
    await passport.connect(seller).transferPassport(0, buyer.address);

    const remaining = await passport.getRemainingWarranty(0);
    const expected = BigInt((365 - 100) * 24 * 60 * 60);
    // toleransi beberapa detik karena block timestamp
    expect(remaining).to.be.closeTo(expected, 60n);
  });

  it("garansi habis mengembalikan 0", async () => {
    const { passport, authenticator, seller } = await loadFixture(deployFixture);
    await mint(passport, authenticator, seller.address);
    await time.increase(400 * 24 * 60 * 60);
    expect(await passport.getRemainingWarranty(0)).to.equal(0);
  });
});
