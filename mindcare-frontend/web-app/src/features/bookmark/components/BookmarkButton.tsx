import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bookmark } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { cn } from "@/shared";
import { bookmarkApi, type BookmarkType } from "../api/bookmark.api";

export function BookmarkButton({ type, targetId, className }: { type: BookmarkType; targetId: string; className?: string }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const authenticated = Boolean(localStorage.getItem("mindcare.accessToken"));
  const bookmarks = useQuery({ queryKey: ["bookmarks"], queryFn: bookmarkApi.list, enabled: authenticated });
  const saved = bookmarks.data?.some((item) => item.targetType === type && item.targetId === targetId) ?? false;
  const mutation = useMutation({
    mutationFn: () => saved ? bookmarkApi.remove(type, targetId) : bookmarkApi.add(type, targetId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["bookmarks"] }),
  });
  return (
    <button type="button" className={cn("grid size-10 place-items-center rounded-full bg-white text-brand-700 shadow-sm disabled:opacity-60", className)} aria-label={saved ? "Bỏ lưu" : "Lưu"} aria-pressed={saved} disabled={mutation.isPending} onClick={(event) => {
      event.preventDefault();
      event.stopPropagation();
      if (!authenticated) navigate("/login");
      else mutation.mutate();
    }}>
      <Bookmark className={cn("size-5", saved && "fill-current")} />
    </button>
  );
}
