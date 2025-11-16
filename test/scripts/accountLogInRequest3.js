(async () => {
    try 
    {
        console.log("Logging into account: \nusername: user3\npassword: password1234\n");

        const response = await fetch("https://umpyr.tech/api/accounts/login", 
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username : "user3", password : "password1234"}),
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
