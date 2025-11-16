(async () => {
    try 
    {
        console.log("Getting ai canned statements\n");

        const response = await fetch("https://umpyr.tech/api/ai/can", 
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ quantity: 4 }),
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