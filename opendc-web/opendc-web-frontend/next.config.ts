import type { NextConfig } from "next"

// trailingSlash stays off: Next would redirect every path without a trailing slash, API requests
// included (/api/v1/config would become /api/v1/config/). The server serves deep links from the export.
const nextConfig: NextConfig = {
    output: "export",
    reactStrictMode: true,
    images: { unoptimized: true },
}

export default nextConfig
