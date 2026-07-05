import { ethers } from "hardhat";

async function main() {
  const [deployer] = await ethers.getSigners();
  console.log("Deploying dengan wallet:", deployer.address);

  const factory = await ethers.getContractFactory("OriginTagPassport");
  const contract = await factory.deploy();
  await contract.waitForDeployment();

  const address = await contract.getAddress();
  console.log("OriginTagPassport deployed:", address);
  console.log("Salin address ini ke backend/.env → CONTRACT_ADDRESS");

  // Beri AUTHENTICATOR_ROLE ke wallet backend jika diset di .env
  const backend = process.env.BACKEND_AUTHENTICATOR_ADDRESS;
  if (backend) {
    const role = await contract.AUTHENTICATOR_ROLE();
    await (await contract.grantRole(role, backend)).wait();
    console.log("AUTHENTICATOR_ROLE diberikan ke:", backend);
  } else {
    console.log("BACKEND_AUTHENTICATOR_ADDRESS kosong — jangan lupa grantRole manual untuk wallet backend");
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
