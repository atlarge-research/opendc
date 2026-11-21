import type { NextConfig } from "next"

const basePath = process.env.NEXT_BASE_PATH

// trailingSlash must stay off. It makes Next 308-redirect every path without a trailing slash,
// including the app's own fetches, so /api/v1/config becomes /api/v1/config/ and hangs instead of
// answering. Serving deep links from the static export is the server's job, not a routing flag's.
const nextConfig: NextConfig = {
    output: "export",
    reactStrictMode: true,
    images: { unoptimized: true },
    basePath: basePath ? `/${basePath}` : undefined,
}

export default nextConfig
