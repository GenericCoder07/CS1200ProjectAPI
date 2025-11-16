(async () => {
    try 
    {
        console.log("Creating new account: \nusername: user2\npassword: password321\nemail: user2@gmail.com\n")

        const response = await fetch("https://umpyr.tech/api/accounts/signin", 
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username : "user2", password : "password321", email : "user2@email.com"}),
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
