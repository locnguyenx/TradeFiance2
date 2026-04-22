import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
});

export const metadata: Metadata = {
  title: "Trade Finance Platform",
  description: "Modern Digital Trade Finance Interface",
};

import { GlobalShell } from "../components/GlobalShell";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={inter.variable}>
      <body>
        <GlobalShell>
          {children}
        </GlobalShell>
      </body>
    </html>
  );
}
