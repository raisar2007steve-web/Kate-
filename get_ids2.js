const channels = [
    { name: "CNA", handle: "@channelnewsasia", id: "" },
    { name: "DW News", handle: "@dwnews", id: "" },
    { name: "WION", handle: "@WION", id: "" },
    { name: "TRT World", handle: "@trtworld", id: "" },
    { name: "NDTV India", handle: "@NDTVIndia", id: "" },
    { name: "EuroNews", handle: "@euronews", id: "" },
    { name: "NBC News", handle: "@NBCNews", id: "" }
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
