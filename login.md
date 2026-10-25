# User Story: Login

As a registered shopper, I want to log in with my username and password
so that I can see the product catalog and check out.

## Acceptance criteria
- A valid username and password takes the user to the inventory page.
- An invalid password shows a clear error message and keeps the user on the login page.
- A locked-out user sees a message saying the account is locked.
- Submitting with an empty username shows a "Username is required" error.
- Submitting with an empty password shows a "Password is required" error.
- Error messages can be dismissed.

## Test data (public demo site)
- Valid: standard_user / secret_sauce
- Locked out: locked_out_user / secret_sauce
