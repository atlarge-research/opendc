import { getById } from "./util";

export function getSection(sectionId) {
  return getById("/sections/{sectionId}", { sectionId });
}
