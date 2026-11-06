"use client";

import Link from "next/link";
import { Compass } from "lucide-react";
import { ROUTES, FOOTER_PRODUCT_LINKS } from "@/constants";

export function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="bg-background text-muted-foreground text-sm border-t border-border/60">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-12 md:py-16">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-10 lg:gap-8">

          {/* Brand Column */}
          <div className="flex flex-col space-y-3">
            <Link href={ROUTES.home} className="flex items-center gap-2.5 w-fit">
              <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary text-primary-foreground">
                <Compass className="h-4 w-4" />
              </div>
              <span className="font-bold text-lg tracking-tight text-foreground">
                WnGo
              </span>
            </Link>
            <p className="text-muted-foreground text-xs leading-relaxed max-w-xs">
              Plan less. Travel more. Remember everything.
            </p>
            <p className="text-xs text-muted-foreground/60 pt-2">
              © {currentYear} WnGo. All rights reserved.
            </p>
          </div>

          {/* Product Links */}
          <div className="flex flex-col space-y-3">
            <h3 className="font-semibold text-foreground text-xs uppercase tracking-wider">
              Sản phẩm
            </h3>
            <ul className="space-y-2.5">
              {FOOTER_PRODUCT_LINKS.map((link, index) => (
                <li key={index}>
                  <Link href={link.href} className="text-muted-foreground hover:text-foreground transition-colors text-xs">
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Company Links */}
          <div className="flex flex-col space-y-3">
            <h3 className="font-semibold text-foreground text-xs uppercase tracking-wider">
              Về chúng tôi
            </h3>
            <ul className="space-y-2.5 text-xs">
              <li>
                <span className="text-muted-foreground">Triết lý thiết kế</span>
              </li>
              <li>
                <span className="text-muted-foreground">Hành trình 63 tỉnh thành</span>
              </li>
              <li>
                <span className="text-muted-foreground">Hỗ trợ & Góp ý</span>
              </li>
            </ul>
          </div>

          {/* Legal Links */}
          <div className="flex flex-col space-y-3">
            <h3 className="font-semibold text-foreground text-xs uppercase tracking-wider">
              Chính sách
            </h3>
            <ul className="space-y-2.5 text-xs">
              <li>
                <span className="text-muted-foreground">Điều khoản sử dụng</span>
              </li>
              <li>
                <span className="text-muted-foreground">Bảo mật dữ liệu</span>
              </li>
            </ul>
          </div>

        </div>
      </div>
    </footer>
  );
}
