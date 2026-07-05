import { HardhatUserConfig } from "hardhat/config";
import "@nomicfoundation/hardhat-toolbox";
import * as dotenv from "dotenv";

dotenv.config();

const PRIVATE_KEY = process.env.PRIVATE_KEY ?? "";

const config: HardhatUserConfig = {
  solidity: {
    version: "0.8.28",
    settings: {
      optimizer: { enabled: true, runs: 200 },
      // OpenZeppelin 5.5.x memakai mcopy (butuh Cancun).
      // BSC & opBNB sudah mendukung Cancun sejak hardfork Haber (2024).
      evmVersion: "cancun",
    },
  },
  networks: {
    bscTestnet: {
      url: process.env.BSC_TESTNET_RPC ?? "https://data-seed-prebsc-1-s1.bnbchain.org:8545",
      chainId: 97,
      accounts: PRIVATE_KEY ? [PRIVATE_KEY] : [],
    },
    // Fase 2 (PRD §9.2): operasi frekuensi tinggi pindah ke opBNB
    opBNBTestnet: {
      url: process.env.OPBNB_TESTNET_RPC ?? "https://opbnb-testnet-rpc.bnbchain.org",
      chainId: 5611,
      accounts: PRIVATE_KEY ? [PRIVATE_KEY] : [],
    },
  },
  etherscan: {
    apiKey: {
      bscTestnet: process.env.BSCSCAN_API_KEY ?? "",
    },
  },
};

export default config;
