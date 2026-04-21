# Feature: Import LC Provision Collection
# As a Trade Finance Officer
# I want to collect provision funds from multiple accounts in different currencies
# So that I can fulfill the LC provision requirement efficiently

Feature: Import LC Provision Collection

  Background:
    Given I am logged in as "tf-admin"
    And I have an LC with ID "DEMO_LC_01" requiring provision of 10000.00 USD
    And the LC applicant has accounts in EUR, GBP, and USD currencies

  # ============================================================================
  # SCENARIO GROUP 1: COLLECTION INITIALIZATION
  # ============================================================================

  Scenario: Initialize provision collection for an LC
    Given I navigate to the LC Financials screen for "DEMO_LC_01"
    When I click "Collect Provision" button
    Then I should see the Provision Collection screen
    And the target provision amount should display "10000.00 USD"
    And the collection status should be "Draft"
    And the total collected amount should be "0.00 USD"

  Scenario: Initialize collection with existing provision
    Given "DEMO_LC_01" already has a provision collection with status "LcPrvColCollected"
    When I navigate to the Provision Collection screen
    Then I should see the existing collection details
    And I should see an option to "Create New Collection" or "View Existing"

  # ============================================================================
  # SCENARIO GROUP 2: ADDING COLLECTION ENTRIES
  # ============================================================================

  Scenario: Add collection entry from EUR account
    Given I am on the Provision Collection screen for "DEMO_LC_01"
    When I select account "ACC_EUR_001" from the account dropdown
    And I enter amount "5000.00" in EUR currency
    Then the system should fetch the EUR/USD exchange rate from CBS
    And display the converted USD amount
    And display the exchange rate used
    And the entry should be added to the collection list
    And the total collected amount should update to reflect the new entry

  Scenario: Add collection entry from GBP account
    Given I am on the Provision Collection screen for "DEMO_LC_01"
    When I select account "ACC_GBP_001" from the account dropdown
    And I enter amount "3000.00" in GBP currency
    Then the system should fetch the GBP/USD exchange rate from CBS
    And display the converted USD amount
    And the entry should be added to the collection list
    And the total collected amount should update

  Scenario: Add collection entry from USD account
    Given I am on the Provision Collection screen for "DEMO_LC_01"
    When I select account "ACC_USD_001" from the account dropdown
    And I enter amount "2000.00" in USD currency
    Then the system should not require exchange rate conversion
    And display the amount as "2000.00 USD"
    And the entry should be added to the collection list

  Scenario: Add multiple collection entries
    Given I am on the Provision Collection screen for "DEMO_LC_01"
    When I add the following collection entries:
      | Account ID   | Currency | Amount    |
      | ACC_EUR_001  | EUR      | 5000.00   |
      | ACC_GBP_001  | GBP      | 3000.00   |
      | ACC_USD_001  | USD      | 2000.00   |
    Then the collection list should show 3 entries
    And the total collected amount should equal the sum of converted USD amounts

  # ============================================================================
  # SCENARIO GROUP 3: VALIDATION AND TOTAL MATCHING
  # ============================================================================

  Scenario: Validate total matches target provision amount
    Given I have added collection entries totaling exactly 10000.00 USD
    When I review the collection summary
    Then the system should show "Total Collected: 10000.00 USD"
    And the system should show "Provision Required: 10000.00 USD"
    And the status should indicate "Complete"
    And the "Collect Funds" button should be enabled

  Scenario: Validate total exceeds target provision amount
    Given I have added collection entries totaling 11000.00 USD
    When I review the collection summary
    Then the system should show a warning "Total exceeds provision requirement"
    And the "Collect Funds" button should be disabled
    And I should be prompted to reduce an entry amount

  Scenario: Validate total is less than target provision amount
    Given I have added collection entries totaling 8000.00 USD
    When I review the collection summary
    Then the system should show "Total Collected: 8000.00 USD"
    And the system should show "Provision Required: 10000.00 USD"
    And the status should indicate "Incomplete"
    And the "Collect Funds" button should be disabled

  Scenario: Validate tolerance for rounding discrepancies
    Given I have added collection entries totaling 9999.99 USD
    When I review the collection summary
    Then the system should accept the total if within tolerance (±0.01 USD)
    And the status should indicate "Complete"
    And the "Collect Funds" button should be enabled

  # ============================================================================
  # SCENARIO GROUP 4: ACCOUNT ELIGIBILITY
  # ============================================================================

  Scenario: Select account owned by LC applicant
    Given I am on the Provision Collection screen for "DEMO_LC_01"
    When I select an account from the dropdown
    Then only accounts owned by the LC applicant should be available
    And accounts from other parties should not appear in the dropdown

  Scenario: Attempt to select account not owned by applicant
    Given I am on the Provision Collection screen for "DEMO_LC_01"
    When I try to select an account not owned by the LC applicant
    Then the system should reject the selection
    And display an error message "Account not owned by LC applicant"

  # ============================================================================
  # SCENARIO GROUP 5: CURRENCY CONVERSION
  # ============================================================================

  Scenario: Fetch exchange rate from CBS
    Given I am adding a collection entry in EUR
    When I enter the amount and move focus away
    Then the system should call CBS get#ExchangeRate service
    And fetch the current EUR/USD exchange rate
    And store the rate with timestamp

  Scenario: Handle unsupported currency
    Given I am adding a collection entry in an unsupported currency (e.g., JPY)
    When I enter the amount
    Then the system should display an error "Currency not supported"
    And prevent adding the entry

  Scenario: Handle CBS exchange rate service failure
    Given CBS exchange rate service is unavailable
    When I try to add a collection entry in EUR
    Then the system should display "Exchange rate service unavailable"
    And prevent adding the entry
    And suggest retrying or manual rate entry

  # ============================================================================
  # SCENARIO GROUP 6: COLLECTING FUNDS
  # ============================================================================

  Scenario: Collect funds from multiple accounts
    Given I have a complete collection (total = 10000.00 USD)
    When I click "Collect Funds" button
    Then the system should call CBS hold#Funds for each account
    And store the CBS hold reference for each entry
    And update each entry status to "Collected"
    And update the collection status to "Collected"
    And display success message with CBS references

  Scenario: Handle partial CBS hold failure
    Given I have a complete collection with 3 entries
    When I click "Collect Funds" and CBS fails on the second account
    Then the system should rollback the first hold
    And display an error message
    And keep the collection in "Draft" status
    And allow retry

  Scenario: Handle all CBS hold failures
    Given I have a complete collection
    When I click "Collect Funds" and all CBS holds fail
    Then the system should display an error
    And keep the collection in "Draft" status
    And log the error details

  # ============================================================================
  # SCENARIO GROUP 7: EDITING AND REMOVING ENTRIES
  # ============================================================================

  Scenario: Edit a collection entry amount
    Given I have an entry with amount 5000.00 EUR
    When I edit the amount to 6000.00 EUR
    Then the system should recalculate the converted USD amount
    And update the total collected amount
    And validate the new total against the target

  Scenario: Remove a collection entry
    Given I have multiple collection entries
    When I remove one entry
    Then the entry should be removed from the list
    And the total collected amount should update
    And the status should recalculate

  Scenario: Cannot edit after funds collected
    Given I have a collection with status "Collected"
    When I try to edit an entry
    Then the system should prevent editing
    And display "Cannot edit after funds collected"

  # ============================================================================
  # SCENARIO GROUP 8: STATUS MANAGEMENT
  # ============================================================================

  Scenario: Collection status transitions
    Given I start with a new collection
    When I add entries totaling less than target
    Then status should be "Draft"
    When I add entries totaling exactly target
    Then status should be "Complete"
    When I collect funds successfully
    Then status should be "Collected"

  Scenario: View collection history
    Given I have multiple collections for an LC
    When I view the LC Financials screen
    Then I should see a list of all collections
    And each collection should show status, date, and total amount

  # ============================================================================
  # SCENARIO GROUP 9: ERROR HANDLING AND EDGE CASES
  # ============================================================================

  Scenario: Concurrent collection attempts
    Given two operators are working on the same LC
    When Operator A starts a collection
    And Operator B tries to start another collection
    Then the system should lock the LC
    And Operator B should see "Collection in progress by another user"

  Scenario: Exchange rate fluctuation during entry
    Given I view the Provision Collection screen
    When the exchange rate changes between viewing and submission
    Then the system should recalculate on submission
    And warn the user if amounts have changed

  Scenario: Handle invalid account ID
    Given I enter an account ID that doesn't exist
    When I try to add the entry
    Then the system should display "Account not found"
    And prevent adding the entry

  Scenario: Handle negative amount entry
    Given I try to enter a negative amount
    When I submit the entry
    Then the system should reject the entry
    And display "Amount must be positive"

  # ============================================================================
  # SCENARIO GROUP 10: INTEGRATION WITH EXISTING FEATURES
  # ============================================================================

  Scenario: Link collection to LC provision
    Given I complete a provision collection
    When I view the LC Financials screen
    Then the provision should be linked to the LC
    And the LC provision status should update

  Scenario: View provision details on LC screen
    Given I have a completed provision collection
    When I view the LC Main screen
    Then I should see provision status
    And a link to view collection details

  Scenario: Integration with CBS simulator
    Given I am using the CBS simulator
    When I collect funds
    Then the simulator should update account balances
    And track the hold references

  # ============================================================================
  # SCENARIO GROUP 11: REVERSAL AND REFUND (PHASE 2)
  # ============================================================================

  Scenario: Release provision funds
    Given I have a collected provision
    When I click "Release Funds" button
    Then the system should call CBS release#Funds for each entry
    And update entry status to "Released"
    And update collection status to "Released"

  Scenario: Partial release of funds
    Given I have a collected provision with multiple entries
    When I release funds for one entry only
    Then that entry status should update to "Released"
    And other entries remain collected

  # ============================================================================
  # SCENARIO GROUP 12: REPORTING AND AUDIT
  # ============================================================================

  Scenario: Audit trail for collection entries
    Given I add a collection entry
    When I view the collection history
    Then I should see who added the entry
    And when it was added
    And any changes made

  Scenario: Generate provision collection report
    Given I have completed collections
    When I generate a report
    Then the report should show all collections
    With details of accounts, amounts, and exchange rates

  # ============================================================================
  # SCENARIO GROUP 13: PERFORMANCE AND SCALABILITY
  # ============================================================================

  Scenario: Handle large number of collection entries
    Given I need to collect from 10+ accounts
    When I add all entries
    Then the system should handle the load efficiently
    And display all entries without performance issues

  Scenario: Batch processing for multiple LCs
    Given I need to collect provisions for multiple LCs
    When I process them in batch
    Then the system should handle batch operations
    And maintain data consistency

  # ============================================================================
  # SCENARIO GROUP 14: SECURITY AND PERMISSIONS
  # ============================================================================

  Scenario: Role-based access to collection screen
    Given I am logged in as "tf-viewer"
    When I try to access the Provision Collection screen
    Then I should be denied access
    And see "Permission denied" message

  Scenario: Maker-checker workflow
    Given I am logged in as "tf-maker"
    When I create a collection
    Then it should be in "Pending Approval" status
    When "tf-checker" approves
    Then the collection can proceed to collection

  # ============================================================================
  # SCENARIO GROUP 15: DATA INTEGRITY
  # ============================================================================

  Scenario: Prevent duplicate collections
    Given I have a completed collection
    When I try to create another collection for the same LC
    Then the system should prevent duplicate
    And suggest viewing the existing collection

  Scenario: Validate data consistency
    Given I have collection entries
    When I verify the data
    Then all entries should sum correctly
    And exchange rates should be stored with entries
    And CBS references should be recorded

  # ============================================================================
  # SCENARIO GROUP 16: USER EXPERIENCE
  # ============================================================================

  Scenario: Real-time total calculation
    Given I am adding collection entries
    When I enter an amount
    Then the total should update immediately
    And show the running total vs. target

  Scenario: Clear visual indicators
    Given I view the collection screen
    Then completed entries should be green
    And incomplete entries should be yellow
    And errors should be red

  Scenario: Helpful error messages
    Given I make an error
    When I try to submit
    Then the system should show clear, actionable error messages
    And suggest how to fix the issue
