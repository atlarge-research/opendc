"use client"

import { openNamePrompt } from "@/components/util/NamePrompt"
import { notifyProblem } from "@/components/util/feedback"
import type { Project } from "@/lib/api/types"
import type { UseMutationResult } from "@tanstack/react-query"
import { useRouter } from "next/navigation"

export function useCreateProjectPrompt(create: UseMutationResult<Project, Error, string>): () => void {
    const router = useRouter()
    return () =>
        openNamePrompt({
            title: "Create a project",
            label: "Project name",
            confirmLabel: "Create",
            onSubmit: (name) =>
                create.mutate(name, {
                    onSuccess: (project) => router.push(`/project?id=${project.id}`),
                    onError: notifyProblem,
                }),
        })
}
