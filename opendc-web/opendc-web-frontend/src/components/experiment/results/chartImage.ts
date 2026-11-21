import type { LegendEntry } from "@/components/experiment/results/SeriesLegend"
import { downloadBlob } from "@/components/util/download"

/** What a saved chart carries besides the plot: its title, caption and legend are drawn outside the SVG. */
export interface ChartImage {
    title: string
    caption: string
    legend: LegendEntry[]
    fileName: string
}

// Twice the size on screen, so the image stays sharp on a slide or a high-density display.
const PIXEL_RATIO = 2
const PADDING = 16
const TITLE_PX = 16
const TEXT_PX = 12
const LINE_HEIGHT = 1.5
const SWATCH_PX = 10
const SWATCH_GAP = 6
const ENTRY_GAP = 16

// What the plot takes from stylesheets, which an SVG drawn on its own no longer sees.
const PAINT = [
    "fill",
    "fill-opacity",
    "stroke",
    "stroke-width",
    "stroke-dasharray",
    "stroke-opacity",
    "opacity",
    "font-family",
    "font-size",
    "font-weight",
]

interface Plot {
    image: HTMLImageElement
    width: number
    height: number
}

interface LegendRow {
    entries: Array<{ entry: LegendEntry; width: number }>
    width: number
}

/** Draws the chart inside [card] with its title, caption and legend, and saves it as a PNG. */
export async function downloadChartImage(card: HTMLElement, image: ChartImage): Promise<void> {
    const surface = card.querySelector("svg.recharts-surface")
    if (!(surface instanceof SVGSVGElement)) throw new Error("There is no chart to save yet")
    const plot = await rasterize(surface)

    const canvas = document.createElement("canvas")
    const context = canvas.getContext("2d")
    if (context === null) throw new Error("This browser cannot draw an image")

    const style = getComputedStyle(card)
    const font = style.fontFamily
    const lineHeight = TEXT_PX * LINE_HEIGHT
    context.font = `${TEXT_PX}px ${font}`
    const legend = legendRows(context, image.legend, plot.width)
    const width = plot.width + 2 * PADDING
    const legendHeight = legend.length === 0 ? 0 : PADDING / 2 + legend.length * lineHeight
    const height = PADDING + TITLE_PX * LINE_HEIGHT + lineHeight + PADDING / 2 + plot.height + legendHeight + PADDING

    canvas.width = Math.ceil(width * PIXEL_RATIO)
    canvas.height = Math.ceil(height * PIXEL_RATIO)
    context.scale(PIXEL_RATIO, PIXEL_RATIO)
    context.fillStyle = style.backgroundColor
    context.fillRect(0, 0, width, height)

    let y = PADDING
    context.textBaseline = "top"
    context.fillStyle = style.color
    context.font = `500 ${TITLE_PX}px ${font}`
    context.fillText(image.title, PADDING, y)
    y += TITLE_PX * LINE_HEIGHT
    context.fillStyle = style.getPropertyValue("--mantine-color-dimmed").trim() || style.color
    context.font = `${TEXT_PX}px ${font}`
    context.fillText(image.caption, PADDING, y)
    y += lineHeight + PADDING / 2
    context.drawImage(plot.image, PADDING, y, plot.width, plot.height)
    y += plot.height + PADDING / 2

    context.textBaseline = "middle"
    for (const row of legend) {
        let x = PADDING + (plot.width - row.width) / 2
        for (const { entry, width: entryWidth } of row.entries) {
            context.fillStyle = entry.color
            context.beginPath()
            context.arc(x + SWATCH_PX / 2, y + lineHeight / 2, SWATCH_PX / 2, 0, 2 * Math.PI)
            context.fill()
            context.fillStyle = style.color
            context.fillText(entry.name, x + SWATCH_PX + SWATCH_GAP, y + lineHeight / 2)
            x += entryWidth + ENTRY_GAP
        }
        y += lineHeight
    }

    const png = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/png"))
    if (png === null) throw new Error("The image could not be encoded")
    downloadBlob(image.fileName, png)
}

async function rasterize(surface: SVGSVGElement): Promise<Plot> {
    const { width, height } = surface.getBoundingClientRect()
    const copy = surface.cloneNode(true)
    if (!(copy instanceof SVGSVGElement)) throw new Error("The chart could not be copied")
    const sources = [surface, ...surface.querySelectorAll("*")]
    const targets = [copy, ...copy.querySelectorAll("*")]
    for (const [index, source] of sources.entries()) {
        const target = targets[index]
        if (!(target instanceof SVGElement)) continue
        const computed = getComputedStyle(source)
        for (const name of PAINT) target.style.setProperty(name, computed.getPropertyValue(name))
    }
    copy.setAttribute("xmlns", "http://www.w3.org/2000/svg")
    copy.setAttribute("width", `${width}`)
    copy.setAttribute("height", `${height}`)

    const image = new Image()
    image.src = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(new XMLSerializer().serializeToString(copy))}`
    await image.decode()
    return { image, width, height }
}

// Entries run left to right and wrap at the plot's width, centred as the legend under the chart is.
function legendRows(context: CanvasRenderingContext2D, legend: LegendEntry[], width: number): LegendRow[] {
    const rows: LegendRow[] = []
    for (const entry of legend) {
        const entryWidth = SWATCH_PX + SWATCH_GAP + context.measureText(entry.name).width
        const last = rows.at(-1)
        if (last !== undefined && last.width + ENTRY_GAP + entryWidth <= width) {
            last.entries.push({ entry, width: entryWidth })
            last.width += ENTRY_GAP + entryWidth
        } else {
            rows.push({ entries: [{ entry, width: entryWidth }], width: entryWidth })
        }
    }
    return rows
}
