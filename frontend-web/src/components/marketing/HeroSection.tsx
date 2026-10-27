"use client";

import Link from "next/link";
import Image from "next/image";
import { ArrowRight, Eye, Heart } from "lucide-react";
import { ROUTES } from "@/constants";

const ADVENTURE_GUIDES = [
  {
    id: "paris-itinerary",
    title: "Paris 5 Day Tourist Itinerary +Rec...",
    description: "I've studied abroad in Paris. Other than that, I've visited Paris another 6 times...",
    author: {
      name: "Elisa Li",
      avatar: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=80",
    },
    views: "105,590",
    likes: "514",
    image: "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=900&auto=format&fit=crop&q=85",
  },
  {
    id: "japan-guide",
    title: "Japan: Video Game Guide 👾",
    description: "Did a summer internship in Japan during 2019. Stayed in Shinagawa City...",
    author: {
      name: "Tuyet",
      avatar: "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=120&auto=format&fit=crop&q=80",
    },
    views: "36,112",
    likes: "691",
    image: "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=900&auto=format&fit=crop&q=85",
  },
  {
    id: "puerto-rico-guide",
    title: "Puerto Rico Guide",
    description: "This is a collection of 'best of' since I moved to the island at the beginning ...",
    author: {
      name: "Gillian Morris",
      avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120&auto=format&fit=crop&q=80",
    },
    views: "28,075",
    likes: "142",
    image: "https://images.unsplash.com/photo-1589553416260-f586c8f1514f?w=900&auto=format&fit=crop&q=85",
  },
  {
    id: "boston-tour",
    title: "Boston Walking Tour",
    description: "I lived in the area for 2 years during gl lived in the area for 2 years during gra...",
    author: {
      name: "DX",
      avatar: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120&auto=format&fit=crop&q=80",
    },
    views: "6,488",
    likes: "91",
    image: "https://images.unsplash.com/photo-1501979376754-2ff867a4f659?w=900&auto=format&fit=crop&q=85",
  },
];

const WORLD_DESTINATIONS = [
  {
    id: "paris",
    name: "Paris",
    places: "Eiffel Tower • Louvre Museum • Notre-Dame Cathedral",
    image: "https://images.unsplash.com/photo-1511739001486-6bfe10ce785f?w=1200&auto=format&fit=crop&q=85",
    colSpan: "col-span-1",
    sizes: "(max-width: 768px) 100vw, 33vw",
  },
  {
    id: "tokyo",
    name: "Tokyo",
    places: "Tokyo Tower • Senso-ji Temple • Shibuya Crossing",
    image: "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=1200&auto=format&fit=crop&q=85",
    colSpan: "col-span-1",
    sizes: "(max-width: 768px) 100vw, 33vw",
  },
  {
    id: "rome",
    name: "Rome",
    places: "Colosseum • Vatican City • Pantheon",
    image: "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=1200&auto=format&fit=crop&q=85",
    colSpan: "col-span-1",
    sizes: "(max-width: 768px) 100vw, 33vw",
  },
  {
    id: "bangkok",
    name: "Bangkok",
    places: "Grand Palace • Wat Arun • Chatuchak Weekend Market",
    image: "https://images.unsplash.com/photo-1563492065599-3520f775eeed?w=1200&auto=format&fit=crop&q=85",
    colSpan: "col-span-1",
    sizes: "(max-width: 768px) 100vw, 33vw",
  },
  {
    id: "new-york",
    name: "New York",
    places: "Statue of Liberty • Central Park • Times Square",
    image: "https://images.unsplash.com/photo-1485871981521-5b1fd3805eee?w=1800&auto=format&fit=crop&q=85",
    colSpan: "col-span-1 md:col-span-2",
    sizes: "(max-width: 768px) 100vw, 67vw",
  },
  {
    id: "london",
    name: "London",
    places: "Tower of London • British Museum • Buckingham Palace",
    image: "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=1800&auto=format&fit=crop&q=85",
    colSpan: "col-span-1 md:col-span-2",
    sizes: "(max-width: 768px) 100vw, 67vw",
  },
  {
    id: "dubai",
    name: "Dubai",
    places: "Burj Khalifa • Dubai Mall • Palm Jumeirah",
    image: "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=1200&auto=format&fit=crop&q=85",
    colSpan: "col-span-1",
    sizes: "(max-width: 768px) 100vw, 33vw",
  },
];

export function HeroSection() {
  return (
    <section className="relative overflow-hidden bg-background pt-10 pb-16 lg:pt-16 lg:pb-24 border-b border-border/60">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">

        {/* ─── Hero Header & Typography ─── */}
        <div className="mx-auto max-w-3xl text-center">


          <h1 className="text-3xl font-bold tracking-tight text-foreground sm:text-4xl lg:text-5xl leading-[1.12] lg:whitespace-nowrap">
            One app for all your travel planning needs
          </h1>

          <p className="mt-5 text-base sm:text-lg text-muted-foreground max-w-2xl mx-auto leading-relaxed">
           Plan your trips, discover new places, and capture every journey — all in one place
          </p>

          <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-4">
            <Link
              href={ROUTES.tripNew}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-lg bg-primary px-6 py-3.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors shadow-sm"
            >
              <span>Start Planning</span>
              <ArrowRight className="h-4 w-4" />
            </Link>
            {/* <Link
              href={ROUTES.journey}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-lg border border-border bg-background px-5 py-3.5 text-sm font-medium text-foreground hover:bg-muted/50 transition-colors"
            >
              <span>Khám phá 63 tỉnh thành</span>
            </Link> */}
          </div>
        </div>

        {/* ─── Explore Places Showcase Grid ─── */}
        <div className="mt-16 sm:mt-24 max-w-7xl mx-auto">
          {/* Section Header */}
          <div className="text-center mb-8 sm:mb-12">
            <h2 className="text-2xl sm:text-3xl lg:text-4xl font-bold tracking-tight text-foreground leading-tight">
              Explore hundreds of places to visit
              <br />
              for every corner of the world
            </h2>
          </div>

          {/* Bento Grid (3 rows: 1-1-1, 1-2, 2-1) */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 sm:gap-4">
            {WORLD_DESTINATIONS.map((item) => (
              <div
                key={item.id}
                className={`relative h-72 sm:h-84 md:h-96 lg:h-[420px] overflow-hidden ${item.colSpan}`}
              >
                <Image
                  src={item.image}
                  alt={item.name}
                  fill
                  className="object-cover"
                  sizes={item.sizes}
                />

                {/* Gradient scrim for high readability */}
                <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/25 to-transparent" />

                {/* Text Content at bottom left */}
                <div className="absolute bottom-4 left-4 right-4 sm:bottom-6 sm:left-6 sm:right-6 text-white">
                  <h3 className="text-xl sm:text-2xl font-bold tracking-tight text-white drop-shadow-sm">
                    {item.name}
                  </h3>
                  <p className="mt-1 text-xs sm:text-sm text-white/90 font-medium leading-snug drop-shadow-sm">
                    {item.places}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* ─── Find Your Next Adventure Section ─── */}
        <div className="mt-24 sm:mt-32 max-w-7xl mx-auto">
          {/* Section Header */}
          <div className="text-center max-w-2xl mx-auto mb-10 sm:mb-12">
            <h2 className="text-2xl sm:text-3xl lg:text-4xl font-bold tracking-tight text-foreground">
              Find your next adventure
            </h2>
            <p className="mt-3 text-sm sm:text-base text-muted-foreground leading-relaxed">
              Browse through itineraries and guides crafted by fellow travelers. Get inspired by real experiences and detailed plans for your next adventure.
            </p>
          </div>

          {/* 4 Cards Grid - No border-radius, no hover effect */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {ADVENTURE_GUIDES.map((guide) => (
              <div key={guide.id} className="flex flex-col">
                {/* Image Container */}
                <div className="relative aspect-square w-full overflow-hidden bg-muted">
                  <Image
                    src={guide.image}
                    alt={guide.title}
                    fill
                    className="object-cover"
                    sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 25vw"
                  />
                </div>

                {/* Content */}
                <div className="pt-3 flex flex-col flex-1">
                  <h3 className="text-base font-bold text-foreground line-clamp-1 leading-snug">
                    {guide.title}
                  </h3>
                  <p className="mt-1 text-xs text-muted-foreground line-clamp-2 leading-relaxed">
                    {guide.description}
                  </p>

                  {/* Author & Stats */}
                  <div className="mt-3 pt-1 flex items-center justify-between text-xs">
                    {/* Author */}
                    <div className="flex items-center gap-2 min-w-0">
                      <div className="relative h-6 w-6 shrink-0 overflow-hidden bg-muted">
                        <Image
                          src={guide.author.avatar}
                          alt={guide.author.name}
                          fill
                          className="object-cover"
                        />
                      </div>
                      <span className="font-medium text-foreground truncate text-xs">
                        {guide.author.name}
                      </span>
                    </div>

                    {/* Stats */}
                    <div className="flex items-center gap-2.5 text-muted-foreground shrink-0 text-[11px]">
                      <span className="flex items-center gap-1">
                        <Eye className="h-3.5 w-3.5" />
                        <span>{guide.views}</span>
                      </span>
                      <span className="flex items-center gap-1">
                        <Heart className="h-3.5 w-3.5" />
                        <span>{guide.likes}</span>
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Action Buttons */}
          <div className="mt-12 sm:mt-14 flex items-center justify-center gap-6">
            <Link
              href={ROUTES.tripNew}
              className="inline-flex items-center justify-center bg-[#ea583a] text-white px-8 py-3 text-sm font-semibold shadow-sm"
            >
              Start planning
            </Link>
            <Link
              href="/#download"
              className="inline-flex items-center gap-1.5 text-sm font-semibold text-foreground"
            >
              <span>Get the app</span>
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </div>

      </div>
    </section>
  );
}
