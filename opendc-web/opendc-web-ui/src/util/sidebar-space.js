export const isCollapsible = (router) =>
    router.asPath.indexOf('portfolios') === -1 && router.asPath.indexOf('scenarios') === -1
