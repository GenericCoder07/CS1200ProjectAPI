(async () => {
    try 
    {
        const sessionToken = process.argv[2];

        const response = await fetch("https://umpyr.tech/api/accounts/signout", 
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ 'session-id' : sessiontoken}),
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
