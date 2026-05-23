const channels = [
    { name: "Aaj Tak", handle: "@aajtak", id: "" },
    { name: "NDTV", handle: "@ndtv", id: "" },
    { name: "India Today", handle: "@indiatoday", id: "" },
    { name: "Republic Bharat", handle: "@RepublicBharat", id: "" },
    { name: "BBC News", handle: "@BBCNews", id: "" },
    { name: "Al Jazeera", handle: "@aljazeeraenglish", id: "" },
    { name: "DW News", handle: "@dwnews", id: "" },
    { name: "Sky News", handle: "@SkyNews", id: "" },
    { name: "ABC News Australia", handle: "@NewsOnABC", id: "" },
    { name: "CNA", handle: "@channelnewsasia", id: "" },
    { name: "France 24", handle: "@France24_en", id: "" },
    { name: "Global News", handle: "@globalnews", id: "" }
];

async function getChannelId(handle) {
    try {
        const res = await fetch(`https://www.youtube.com/${handle}`);
        const text = await res.text();
        const match = text.match(/"channelId":"(UC[^"]+)"/);
        return match ? match[1] : null;
    } catch(e) {
        return null;
    }
}

async function run() {
    for (const c of channels) {
        c.id = await getChannelId(c.handle);
        console.log(`YouTubeChannel("${c.name}", "${c.id}", "${c.name.substring(0,2).toUpperCase()}", "🔴 LIVE", "Global"),`);
    }
}
run();
