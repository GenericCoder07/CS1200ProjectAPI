(async () => {
    try 
    {
        console.log("Getting pros and cons list with a statement of:\n\"Desktop computers are way better than laptop computers.\"\n");

        const response = await fetch("https://umpyr.tech/api/ai", 
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ msg: "Desktop computers are way better than laptop computers." }),
            });

        if (!response.ok) 
        {
            console.error("HTTP error:", response.status, await response.text());
            return;
        }

        const data = await response.json();
        console.log("Response JSON:", data);
    } 
    catch (err) 
    {
        console.error("Fetch failed:", err);
    }
})();