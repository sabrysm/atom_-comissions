# atom_-comissions

## Commission 4: Lootsplit Bot
### Price: $120

#### lootsplit commands:

1. **/bal**
    - Returns the balance of the user.
    - **Condition:** Ensure the user is registered before displaying the balance.

2. **/lootsplit create (name)**
    - Creates a new lootsplit session with the specified name.

3. **/lootsplit party upload (img)**
    - Uploads the loot split for the party.

4. **/lootsplit guild upload (name) (file)**
    - Uploads the loot split for the guild. If no upload, uses the default setup from `/guild upload`.

5. **/lootsplit add (player)**
    - Adds a player to the current lootsplit session.

6. **/lootsplit remove (player)**
    - Removes a player from the current lootsplit session.

7. **/lootsplit set value (silver) (items)**
    - Sets the value for silver and items to be split.

8. **/lootsplit half (player_name)**
    - Player only gets half of the amount added to their balance.

9. **/lootsplit confirm (split-ID)**
    - Confirms the lootsplit with the specified ID and adds balances after performing calculations based on:
        - Amount added: `silver amount * 0.9 + item value * 0.75`.
        - This calculation should be editable in the code (commented as such).

10. **/remove user amount (amount)**
    - Removes a specified amount from a user’s balance.

11. **/give user amount (amount)**
    - Adds a specified amount to a user’s balance.

12. **/leaderboard**
    - Shows the leaderboard with the highest balance in a format similar to UnbelievaBoat’s.


#### User Management:

1. **/register (in-game username)**
    - Registers the user with their in-game username.
    - Cross-check the username with the uploaded guild list to verify they exist.
    - Ensure the username hasn’t already been registered.
    - Output respective messages for success or failure (e.g., already registered, username not found in guild list).
2. **/guild upload (name) (file)**
    - Uploads the guild list, which can be used for cross-referencing. Stored ONCE per server
3. **/guild remove (name)**
    - Removes the guild list
4. **/guestsetup (role) (time_in_min)**
    - Sets a role as the "guest" role that grants that role for that time
    - Listens for role change, after user receives that guest role,t hey will hold it for Time time.


#### Additional Notes:

- **Embeds:** Lootsplit results will generate an embed with multiple pages:
    - **Page 1:** Party information.
    - **Page 2:** Loot image displaying silver/item info, and the final split details.
