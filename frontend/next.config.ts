import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  allowedDevOrigins: ["127.0.0.1", "localhost"],
  async rewrites() {
    return [
      {
        source: '/rest/:path*',
        destination: 'http://localhost:8080/rest/:path*',
      },
    ];
  },
};

export default nextConfig;
