import { ethers } from "hardhat";

async function main() {
  const [deployer] = await ethers.getSigners();
  console.log("Deploying dengan wallet:", deployer.address);

  const factory = await ethers.getContractFactory("OriginTagPassport");
  const contract = await factory.deploy();
  await contract.waitForDeployment();

  const address = await contract.getAddress();
  console.log("OriginTagPassport deployed:", address);

  // Beri AUTHENTICATOR_ROLE ke wallet backend jika diset di .env
  const backend = process.env.BACKEND_AUTHENTICATOR_ADDRESS;
  if (backend) {
    const role = await contract.AUTHENTICATOR_ROLE();
    await (await contract.grantRole(role, backend)).wait();
    console.log("AUTHENTICATOR_ROLE diberikan ke:", backend);
  } else {
    console.log("BACKEND_AUTHENTICATOR_ADDRESS kosong — jangan lupa grantRole manual untuk wallet backend");
  }

  // Deploy marketplace (escrow) yang menunjuk ke passport ini.
  const feeRecipient = process.env.MARKETPLACE_FEE_RECIPIENT ?? deployer.address;
  const feeBps = Number(process.env.MARKETPLACE_FEE_BPS ?? 200); // 2%
  const marketFactory = await ethers.getContractFactory("OriginTagMarketplace");
  const market = await marketFactory.deploy(address, feeRecipient, feeBps);
  await market.waitForDeployment();
  const marketAddress = await market.getAddress();
  console.log("OriginTagMarketplace deployed:", marketAddress);

  console.log("\n=== Update backend/.env ===");
  console.log(`CONTRACT_ADDRESS=${address}`);
  console.log(`MARKETPLACE_ADDRESS=${marketAddress}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
