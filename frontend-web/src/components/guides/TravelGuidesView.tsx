"use client";

import { useState, useMemo, useEffect } from "react";
import { Search, Heart, Eye, Share2, Check, Compass, X } from "lucide-react";
import {
  INITIAL_GUIDES_DATA,
  MORE_GUIDES_DATA,
  DESTINATION_TAGS_DATA,
  GUIDE_QUICK_CHIPS,
  type GuideItem,
} from "@/constants";

export function TravelGuidesView() {
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedTag, setSelectedTag] = useState<string | null>(null);
  const [hasLoadedMore, setHasLoadedMore] = useState(false);
  const [likedGuides, setLikedGuides] = useState<Record<string, boolean>>({});
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [shareError, setShareError] = useState("");
  useEffect(() => {
    const reveal = () => {
      if (window.location.hash) {
        setHasLoadedMore(true);
        setSearchQuery("");
        setSelectedTag(null);
      }
    };
    reveal();
    window.addEventListener("hashchange", reveal);
    return () => window.removeEventListener("hashchange", reveal);
  }, []);
  useEffect(() => {
    if (hasLoadedMore && window.location.hash)
      document
        .getElementById(decodeURIComponent(window.location.hash.slice(1)))
        ?.scrollIntoView();
  }, [hasLoadedMore]);

  // Combine initial and loaded guides
  const allGuides = useMemo(() => {
    return hasLoadedMore
      ? [...INITIAL_GUIDES_DATA, ...MORE_GUIDES_DATA]
      : INITIAL_GUIDES_DATA;
  }, [hasLoadedMore]);

  // Filter guides based on search query or tag
  const filteredGuides = useMemo(() => {
    let result = allGuides;

    if (selectedTag) {
      const tagLower = selectedTag.toLowerCase();
      result = result.filter(
        (g) =>
          g.destination.toLowerCase().includes(tagLower) ||
          g.title.toLowerCase().includes(tagLower),
      );
    }

    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase().trim();
      result = result.filter(
        (g) =>
          g.title.toLowerCase().includes(query) ||
          g.destination.toLowerCase().includes(query) ||
          g.description.toLowerCase().includes(query) ||
          g.author.name.toLowerCase().includes(query),
      );
    }

    return result;
  }, [allGuides, searchQuery, selectedTag]);

  // Handle like toggle
  const toggleLike = (id: string, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setLikedGuides((prev) => ({
      ...prev,
      [id]: !prev[id],
    }));
  };

  // Handle share (copy link)
  const handleShare = async (guide: GuideItem, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (typeof window !== "undefined") {
      try {
        await navigator.clipboard.writeText(
          `${window.location.origin}/travel-guides#${guide.id}`,
        );
        setCopiedId(guide.id);
        setTimeout(() => setCopiedId(null), 2000);
      } catch {
        setShareError(
          "Không thể sao chép. Bạn có thể sao chép liên kết trên thanh địa chỉ.",
        );
        window.location.hash = guide.id;
      }
    }
  };

  // Scroll to browse section
  const handleScrollToBrowse = () => {
    const el = document.getElementById("browse-destinations");
    if (el) {
      el.scrollIntoView({ behavior: "smooth" });
    }
  };

  // Select a tag filter
  const handleSelectTag = (tag: string) => {
    if (selectedTag === tag) {
      setSelectedTag(null);
    } else {
      setSelectedTag(tag);
      setSearchQuery("");
    }
  };

  return (
    <div className="min-h-screen bg-white">
      {shareError && (
        <p role="alert" className="p-3 text-sm text-destructive">
          {shareError}
        </p>
      )}
      {/* ─── 2. Hero Search & Quick Chips Section ─── */}
      <section className="relative overflow-hidden bg-gradient-to-b from-sky-50/70 via-cyan-50/30 to-white py-14 sm:py-20 border-b border-slate-100">
        <div className="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8 text-center">
          {/* Section Title */}
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-slate-900 tracking-tight leading-tight">
            Explore travel guides and itineraries
          </h1>
          <p className="mt-3 text-sm sm:text-base text-slate-600 max-w-2xl mx-auto">
            Khám phá hàng ngàn cẩm nang du lịch chất lượng cao và lịch trình chi
            tiết được chia sẻ bởi cộng đồng lữ khách toàn cầu.
          </p>

          {/* Central Search Bar */}
          <div className="mt-8 relative max-w-2xl mx-auto">
            <div className="relative flex items-center shadow-lg shadow-cyan-900/5 rounded-2xl overflow-hidden border border-cyan-100 bg-white">
              <Search className="h-5 w-5 text-cyan-600 ml-4 shrink-0" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  if (selectedTag) setSelectedTag(null);
                }}
                placeholder="Search for a destination (e.g. Japan, New York, Đà Nẵng...)"
                className="w-full px-3.5 py-4 text-sm sm:text-base text-slate-900 placeholder:text-slate-400 focus:outline-none"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery("")}
                  className="mr-3 p-1 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>
          </div>

          {/* Quick Suggestion Chips */}
          <div className="mt-6 flex flex-wrap items-center justify-center gap-2 text-xs sm:text-sm">
            <span className="text-slate-500 font-normal mr-1">
              Or browse our most popular destinations:
            </span>

            {GUIDE_QUICK_CHIPS.slice(0, 5).map((chip) => {
              const isSelected = selectedTag === chip;
              return (
                <button
                  key={chip}
                  onClick={() => handleSelectTag(chip)}
                  className={`rounded-full px-3 py-1 font-medium transition ${
                    isSelected
                      ? "bg-cyan-500 text-white shadow-sm shadow-cyan-500/30"
                      : "bg-slate-100 text-slate-700 hover:bg-slate-200"
                  }`}
                >
                  {chip}
                </button>
              );
            })}

            {/* See more... link that smooth scrolls to section 4 */}
            <button
              onClick={handleScrollToBrowse}
              className="rounded-full bg-slate-100 px-3 py-1 font-medium text-slate-600 hover:bg-slate-200 hover:text-cyan-600 transition"
            >
              See more...
            </button>
          </div>

          {/* Active filter badge */}
          {(selectedTag || searchQuery) && (
            <div className="mt-4 inline-flex items-center gap-2 rounded-full bg-cyan-50 border border-cyan-200 px-3 py-1 text-xs text-cyan-800 animate-fade-in">
              <span>
                Đang lọc theo: <b>{searchQuery || selectedTag}</b> (
                {filteredGuides.length} cẩm nang)
              </span>
              <button
                onClick={() => {
                  setSelectedTag(null);
                  setSearchQuery("");
                }}
                className="p-0.5 rounded-full hover:bg-cyan-200"
              >
                <X className="h-3 w-3" />
              </button>
            </div>
          )}
        </div>
      </section>

      {/* ─── 3. Recent Guides Grid ─── */}
      <section className="py-12 sm:py-16">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          {/* Section Header */}
          <div className="flex items-center justify-between mb-8">
            <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Recent guides
            </h2>
            <span className="text-xs sm:text-sm font-semibold text-slate-500">
              Hiển thị {filteredGuides.length} cẩm nang
            </span>
          </div>

          {/* Guides 3x4 Grid */}
          {filteredGuides.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-slate-300 py-16 text-center">
              <Compass className="h-10 w-10 text-slate-300 mx-auto mb-3" />
              <p className="font-bold text-slate-700">
                Không tìm thấy cẩm nang phù hợp
              </p>
              <p className="text-xs text-slate-500 mt-1">
                Thử tìm kiếm với từ khóa khác hoặc xóa bộ lọc.
              </p>
              <button
                onClick={() => {
                  setSelectedTag(null);
                  setSearchQuery("");
                }}
                className="mt-4 text-xs font-bold text-cyan-600 hover:underline"
              >
                Xem tất cả cẩm nang
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
              {filteredGuides.map((guide) => {
                const isLiked = likedGuides[guide.id];
                const likeCount = isLiked ? guide.likes + 1 : guide.likes;
                const isCopied = copiedId === guide.id;

                return (
                  <div
                    key={guide.id}
                    id={guide.id}
                    className="group flex flex-col justify-between rounded-2xl border border-slate-200/90 bg-white overflow-hidden shadow-sm hover:shadow-xl hover:border-slate-300 transition-all duration-300"
                  >
                    {/* Top part: Image + Details */}
                    <div>
                      {/* Image Thumbnail (No hover zoom) */}
                      <div className="relative aspect-[16/10] w-full overflow-hidden bg-slate-100">
                        <img
                          src={guide.image}
                          alt={guide.title}
                          className="h-full w-full object-cover"
                        />

                        {/* Top Action Buttons (Share & More) */}
                        <div className="absolute top-2.5 right-2.5 flex items-center gap-1.5 z-10">
                          {/* Share button */}
                          <button
                            onClick={(e) => handleShare(guide, e)}
                            className="flex items-center gap-1 rounded-full bg-black/50 backdrop-blur-md px-2.5 py-1 text-[11px] font-semibold text-white hover:bg-black/75 transition shadow-sm"
                            title="Chia sẻ cẩm nang"
                          >
                            <Share2 className="h-3 w-3" />
                            <span>{isCopied ? "Đã chép!" : "Share"}</span>
                          </button>
                        </div>
                      </div>

                      {/* Card Content */}
                      <div className="p-4">
                        {/* Title with Verified Badge */}
                        <div className="flex items-start gap-1.5">
                          <h3 className="font-bold text-sm text-slate-900 line-clamp-1 group-hover:text-cyan-600 transition-colors">
                            {guide.title}
                          </h3>
                          {guide.verified && (
                            <span
                              className="inline-flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-cyan-600 text-white mt-0.5"
                              title="Cẩm nang đã được xác thực"
                            >
                              <Check className="h-2.5 w-2.5 stroke-[3]" />
                            </span>
                          )}
                        </div>

                        {/* Description */}
                        <p className="mt-1.5 text-xs text-slate-500 line-clamp-2 leading-relaxed">
                          {guide.description}
                        </p>
                      </div>
                    </div>

                    {/* Card Footer: Author & Social stats */}
                    <div className="px-4 pb-4 pt-1 flex items-center justify-between border-t border-slate-100 text-xs text-slate-500 mt-2">
                      {/* Author */}
                      <div className="flex items-center gap-2 min-w-0 pr-2">
                        <img
                          src={guide.author.avatar}
                          alt={guide.author.name}
                          className="h-5 w-5 rounded-full object-cover shrink-0 border border-slate-200"
                        />
                        <span className="font-medium text-slate-700 truncate text-[11px]">
                          {guide.author.name}
                        </span>
                      </div>

                      {/* Stats: Hearts & Views */}
                      <div className="flex items-center gap-3 shrink-0 text-[11px] font-medium text-slate-500">
                        {/* Like button */}
                        <button
                          onClick={(e) => toggleLike(guide.id, e)}
                          className={`flex items-center gap-1 hover:text-rose-600 transition ${
                            isLiked ? "text-rose-600 font-bold" : ""
                          }`}
                          title="Yêu thích"
                        >
                          <Heart
                            className={`h-3.5 w-3.5 ${
                              isLiked
                                ? "fill-rose-600 text-rose-600"
                                : "text-slate-400"
                            }`}
                          />
                          <span>{likeCount}</span>
                        </button>

                        {/* Views */}
                        <div className="flex items-center gap-1 text-slate-400">
                          <Eye className="h-3.5 w-3.5 text-slate-400" />
                          <span>
                            {guide.views > 999
                              ? `${(guide.views / 1000).toFixed(0)}k`
                              : guide.views}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Load More Button */}
          {!hasLoadedMore && filteredGuides.length > 0 && (
            <div className="mt-12 text-center">
              <button
                onClick={() => setHasLoadedMore(true)}
                className="inline-flex items-center justify-center rounded-full border border-slate-300 bg-white px-8 py-2.5 text-sm font-semibold text-slate-800 shadow-sm hover:bg-slate-50 hover:border-slate-400 active:scale-95 transition-all"
              >
                See more
              </button>
            </div>
          )}
        </div>
      </section>

      {/* ─── 4. Browse Destinations Tag Cloud ─── */}
      <section
        id="browse-destinations"
        className="py-14 sm:py-18 bg-gradient-to-b from-slate-50 via-cyan-50/20 to-sky-50/50 border-t border-slate-200/80"
      >
        <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
          <div className="text-left mb-8">
            <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900">
              Or browse our destinations with the most guides
            </h2>
            <p className="text-xs sm:text-sm text-slate-500 mt-1">
              Chọn điểm đến để khám phá các cẩm nang du lịch và gợi ý lịch trình
              chi tiết nhất.
            </p>
          </div>

          {/* Tag Cloud Pills */}
          <div className="flex flex-wrap gap-2.5">
            {DESTINATION_TAGS_DATA.map((tag) => {
              const isSelected = selectedTag === tag;
              return (
                <button
                  key={tag}
                  onClick={() => {
                    handleSelectTag(tag);
                    window.scrollTo({ top: 0, behavior: "smooth" });
                  }}
                  className={`rounded-full px-4 py-2 text-xs sm:text-sm font-medium transition-all ${
                    isSelected
                      ? "bg-cyan-500 text-white shadow-md shadow-cyan-500/25"
                      : "bg-white border border-slate-200/90 text-slate-700 hover:bg-cyan-50 hover:text-cyan-700 hover:border-cyan-200 shadow-sm"
                  }`}
                >
                  {tag}
                </button>
              );
            })}
          </div>
        </div>
      </section>
    </div>
  );
}
