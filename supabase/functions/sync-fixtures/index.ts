// Supabase Edge Function: sync-fixtures
// Automatically ingests upcoming and live sports fixtures into public.sports_fixtures
// using ESPN's public scoreboard feeds (real-time live status, minutes, scores, and HD logos).
// Covers: Primeira Liga, Premier League, La Liga, Serie A, Champions League, Formula 1, and NBA.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

interface SportsFixtureRecord {
  id: string;
  sport: "football" | "motorsport" | "basketball" | "tennis" | "other";
  competition: string;
  competition_logo_url: string | null;
  home_team: string;
  away_team: string;
  home_team_logo_url: string | null;
  away_team_logo_url: string | null;
  kickoff_utc: string; // ISO-8601
  broadcaster_official: string;
  status: "scheduled" | "live" | "finished" | "postponed";
  minute?: string | null;
  home_score?: number | null;
  away_score?: number | null;
  venue?: string | null;
  updated_at: string;
}

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

// ESPN Soccer leagues
const SOCCER_LEAGUES = [
  {
    slug: "por.1",
    name: "Primeira Liga",
    defaultBroadcaster: "Sport TV 1",
    logo: "https://r2.thesportsdb.com/images/media/league/badge/vuytsr1549891157.png"
  },
  {
    slug: "eng.1",
    name: "Premier League",
    defaultBroadcaster: "DAZN 1",
    logo: "https://r2.thesportsdb.com/images/media/league/badge/dsx7v71549891040.png"
  },
  {
    slug: "esp.1",
    name: "La Liga",
    defaultBroadcaster: "DAZN 2",
    logo: "https://r2.thesportsdb.com/images/media/league/badge/yptxwq1549891124.png"
  },
  {
    slug: "ita.1",
    name: "Serie A",
    defaultBroadcaster: "Sport TV 2",
    logo: "https://r2.thesportsdb.com/images/media/league/badge/e8q1i21549891147.png"
  },
  {
    slug: "uefa.champions",
    name: "UEFA Champions League",
    defaultBroadcaster: "DAZN 1",
    logo: "https://upload.wikimedia.org/wikipedia/en/b/bf/UEFA_Champions_League_logo_2.svg"
  },
  {
    slug: "uefa.europa",
    name: "UEFA Europa League",
    defaultBroadcaster: "Sport TV 1",
    logo: "https://r2.thesportsdb.com/images/media/league/badge/28741513623681.png"
  }
];

const BROWSER_HEADERS = {
  "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
  "Accept": "application/json"
};

async function fetchSoccerScoreboard(league: typeof SOCCER_LEAGUES[0]): Promise<SportsFixtureRecord[]> {
  try {
    const url = `https://site.api.espn.com/apis/site/v2/sports/soccer/${league.slug}/scoreboard`;
    const res = await fetch(url, { headers: BROWSER_HEADERS });
    if (!res.ok) return [];
    const data = await res.json();
    const events = data?.events;
    if (!Array.isArray(events)) return [];

    const now = new Date();
    const results: SportsFixtureRecord[] = [];

    for (const ev of events) {
      const comp = ev.competitions?.[0];
      if (!comp) continue;

      const competitors = comp.competitors;
      if (!Array.isArray(competitors) || competitors.length < 2) continue;

      const homeComp = competitors.find((c: any) => c.homeAway === "home") ?? competitors[0];
      const awayComp = competitors.find((c: any) => c.homeAway === "away") ?? competitors[1];

      const homeTeam = homeComp.team?.displayName || homeComp.team?.name || "Home";
      const awayTeam = awayComp.team?.displayName || awayComp.team?.name || "Away";
      const homeLogo = homeComp.team?.logo || null;
      const awayLogo = awayComp.team?.logo || null;

      const state = comp.status?.type?.state; // "pre", "in", "post"
      const status: "scheduled" | "live" | "finished" =
        state === "in" ? "live" : state === "post" ? "finished" : "scheduled";

      const shortDetail = comp.status?.type?.shortDetail;
      const minute = status === "live" ? (shortDetail || "EM DIRETO") : null;

      const homeScore = homeComp.score ? parseInt(homeComp.score, 10) : null;
      const awayScore = awayComp.score ? parseInt(awayComp.score, 10) : null;

      // Portuguese broadcaster rules:
      let broadcaster = league.defaultBroadcaster;
      if (league.slug === "por.1" && /benfica/i.test(homeTeam)) {
        broadcaster = "BTV";
      }

      results.push({
        id: `espn-${ev.id}`,
        sport: "football",
        competition: league.name,
        competition_logo_url: league.logo,
        home_team: homeTeam,
        away_team: awayTeam,
        home_team_logo_url: homeLogo,
        away_team_logo_url: awayLogo,
        kickoff_utc: new Date(ev.date).toISOString(),
        broadcaster_official: broadcaster,
        status,
        minute,
        home_score: !isNaN(Number(homeScore)) ? homeScore : null,
        away_score: !isNaN(Number(awayScore)) ? awayScore : null,
        venue: comp.venue?.fullName || null,
        updated_at: now.toISOString()
      });
    }

    return results;
  } catch (err) {
    console.error(`Error fetching ESPN scoreboard for ${league.name}:`, err);
    return [];
  }
}

async function fetchFormula1Events(): Promise<SportsFixtureRecord[]> {
  try {
    const url = "https://site.api.espn.com/apis/site/v2/sports/racing/f1/scoreboard";
    const res = await fetch(url, { headers: BROWSER_HEADERS });
    if (!res.ok) return [];
    const data = await res.json();
    const events = data?.events;
    if (!Array.isArray(events)) return [];

    const now = new Date();
    const fixtures: SportsFixtureRecord[] = [];

    for (const ev of events) {
      const comp = ev.competitions?.[0];
      const kickoff = new Date(ev.date);
      const isLive = kickoff.getTime() <= now.getTime() && (now.getTime() - kickoff.getTime() < 3 * 3600 * 1000);

      fixtures.push({
        id: `espn-f1-${ev.id}`,
        sport: "motorsport",
        competition: "Formula 1",
        competition_logo_url: "https://r2.thesportsdb.com/images/media/league/badge/g8cofl1513623681.png",
        home_team: ev.name || "Formula 1 Grand Prix",
        away_team: "Corrida Principal",
        home_team_logo_url: "https://r2.thesportsdb.com/images/media/event/poster/po5plo1787424677.jpg",
        away_team_logo_url: "https://r2.thesportsdb.com/images/media/league/badge/g8cofl1513623681.png",
        kickoff_utc: kickoff.toISOString(),
        broadcaster_official: "DAZN 1",
        status: isLive ? "live" : "scheduled",
        minute: isLive ? "EM DIRETO" : null,
        venue: comp?.venue?.fullName || "Circuito",
        updated_at: now.toISOString()
      });
    }

    return fixtures;
  } catch (err) {
    console.error("Error fetching ESPN F1 events:", err);
    return [];
  }
}

async function fetchNbaEvents(): Promise<SportsFixtureRecord[]> {
  try {
    const url = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard";
    const res = await fetch(url, { headers: BROWSER_HEADERS });
    if (!res.ok) return [];
    const data = await res.json();
    const events = data?.events;
    if (!Array.isArray(events)) return [];

    const now = new Date();
    const fixtures: SportsFixtureRecord[] = [];

    for (const ev of events) {
      const comp = ev.competitions?.[0];
      if (!comp) continue;

      const competitors = comp.competitors;
      if (!Array.isArray(competitors) || competitors.length < 2) continue;

      const homeComp = competitors.find((c: any) => c.homeAway === "home") ?? competitors[0];
      const awayComp = competitors.find((c: any) => c.homeAway === "away") ?? competitors[1];

      const state = comp.status?.type?.state;
      const status = state === "in" ? "live" : state === "post" ? "finished" : "scheduled";
      const shortDetail = comp.status?.type?.shortDetail;

      fixtures.push({
        id: `espn-nba-${ev.id}`,
        sport: "basketball",
        competition: "NBA",
        competition_logo_url: "https://r2.thesportsdb.com/images/media/league/badge/14291513623681.png",
        home_team: homeComp.team?.displayName || "Home",
        away_team: awayComp.team?.displayName || "Away",
        home_team_logo_url: homeComp.team?.logo || null,
        away_team_logo_url: awayComp.team?.logo || null,
        kickoff_utc: new Date(ev.date).toISOString(),
        broadcaster_official: "Sport TV 2",
        status,
        minute: status === "live" ? shortDetail : null,
        home_score: homeComp.score ? parseInt(homeComp.score, 10) : null,
        away_score: awayComp.score ? parseInt(awayComp.score, 10) : null,
        venue: comp.venue?.fullName || null,
        updated_at: now.toISOString()
      });
    }

    return fixtures;
  } catch (err) {
    console.error("Error fetching ESPN NBA events:", err);
    return [];
  }
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, {
      status: 204,
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
        "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type"
      }
    });
  }

  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) {
    return new Response(JSON.stringify({ error: "Missing Supabase credentials in Edge Function environment" }), {
      status: 500,
      headers: { "Content-Type": "application/json" }
    });
  }

  const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);
  const allFixtures: SportsFixtureRecord[] = [];
  const now = new Date();

  // 1. Fetch Soccer across all major European leagues
  for (const league of SOCCER_LEAGUES) {
    const list = await fetchSoccerScoreboard(league);
    allFixtures.push(...list);
  }

  // 2. Fetch Formula 1
  const f1List = await fetchFormula1Events();
  allFixtures.push(...f1List);

  // 3. Fetch NBA
  const nbaList = await fetchNbaEvents();
  allFixtures.push(...nbaList);

  console.log(`[sync-fixtures] Collected ${allFixtures.length} real fixtures across football, F1, and NBA.`);

  if (allFixtures.length === 0) {
    return new Response(JSON.stringify({ success: true, count: 0, message: "No upcoming events found from upstream feeds" }), {
      headers: { "Content-Type": "application/json" }
    });
  }

  // Upsert into public.sports_fixtures
  const { data, error } = await supabase
    .from("sports_fixtures")
    .upsert(allFixtures, { onConflict: "id" });

  if (error) {
    console.error("[sync-fixtures] Upsert failed:", error);
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { "Content-Type": "application/json" }
    });
  }

  // Cleanup past events finished more than 24 hours ago
  const cutoff = new Date(now.getTime() - 24 * 3600 * 1000).toISOString();
  await supabase
    .from("sports_fixtures")
    .delete()
    .lt("kickoff_utc", cutoff);

  return new Response(JSON.stringify({
    success: true,
    syncedCount: allFixtures.length,
    sports: ["football", "motorsport", "basketball"],
    timestamp: now.toISOString()
  }), {
    headers: { "Content-Type": "application/json" }
  });
});
