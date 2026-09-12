import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "img.kleinanzeigen.de",
        pathname: "/api/v1/**",
      },
      {
        protocol: "https",
        hostname: "static.kleinanzeigen.de",
        pathname: "/**",
      },
    ],
  },
};

export default nextConfig;
