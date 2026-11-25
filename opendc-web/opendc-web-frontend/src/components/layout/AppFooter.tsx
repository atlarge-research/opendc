import { Text } from "@mantine/core"

export function AppFooter() {
    const year = new Date().getFullYear()
    return (
        <Text component="footer" c="dimmed" size="sm" ta="center" py="md">
            &copy; {year} AtLarge Research
        </Text>
    )
}
