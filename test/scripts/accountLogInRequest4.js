(async () => {
    try 
    {
        console.log("Logging into account: \nusername: user4\npassword: password12345\n");

        const response = await fetch("https://umpyr.tech/api/accounts/login", 
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username : "user4", password : "password12345"}),
            });

        if (!response.ok) 
        {
            console.error("HTTP error:", response.status, await response.text());
            return;
        }

        const data = await response.json();
        
        console.log("Response JSON:", data, "\n");
    } 
    catch (err) 
    {
        console.error("Fetch failed:", err);
    }
})();
