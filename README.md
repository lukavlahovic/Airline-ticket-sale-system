# Sale of airline tickets 
Project for class "Software Components" at Računarski fakultet. Worked on by ![lukijan98](https://github.com/lukijan98), ![apetrovic997](https://github.com/apetrovic997) and ![lukavlahovic](https://github.com/lukavlahovic).

## Brief description of the system:

The system consists of 3 services. These services include the User Service that the user uses to log in to the system and receives a user token with which he is further authorized each time he addresses the system. Also a flight service that is responsible for the management and administration of flights. And a ticket service.

## Functionalities:

● Service 1 (Customer Service):\
○ Registration - Name, surname, email, code, passport number (at the end of registration the user is sent an email to verify the email address).\
○ Login - the user forwards the email and password, if the login is successful he receives a JWT token in response.\
○ Edit profile - when the user logs into the system has the ability to change all its parameters. If you change the email address, you need to send a verification message to the new email.\
○ Credit card - the user can add 1 or more credit cards to the system. The credit card of the parameters has: Name and Surname of the owner, card number and security number of 3 digits.\
○ Ranks - Users are assigned a certain rank depending on the miles that are assigned when buying tickets. The ranks are Gold (> 10000 mi), Silver (> 1000 mi), Bronze (<1000 mi).\
○ Admin - In addition to the regular user, there is also an admin, whose role is to add and delete flights. The admin of the parameters has only a username and a password.\
Administrators are inserted into the system at initial startup and their parameters can be predefined. (admin accounts are hardcoded into the system).

● Service 2 (Flight Service):\
○ Flight list - The flight list is displayed to the user. Flight parameters are airplane, starting destination, final destination, flight length, price. The user is not shown flights whose passenger capacity is full.\
○ Flight Search - The user can search for flights based on all parameters.\
○ When showing flights, it is necessary to do pagination. So not all flights are loaded at once but part by part depending on the query parameters.\
○ Adding and deleting airplanes - The administrator can add and delete airplanes. The parameter plane has the name and passenger capacity. However, it can only delete a plane if it has not been added to any flight.\
○ Adding and deleting flights - The administrator can add and delete flights, but when deleting if at least one ticket is sold then a refund is made for the flight (There is no literal refund because the payment is only simulated, here the user should be notified by email, take away his miles and mark the flight and tickets as canceled). When refunding money, the information is propagated to service 1 and service 3 via a message broker.

● Service 3 (Airline Ticket Service):\
○ Ticket purchase - The user can purchase tickets for existing flights through this service. When making a purchase, the user chooses the credit card with which he wants to pay, if he did not enter any card in the system before the purchase, then at the end of the purchase he will be required to enter the card with which he wants to pay. If the capacity of the aircraft for a given flight is full, the user will be shown an error during the purchase. Depending on the rank of the user, he gets a discount on the ticket price (gold-20%, silver-10%, bronze-0%). After a successful purchase, the user's miles are updated. The information card contains user information, flight information and date of purchase.\
○ Overview of purchased tickets - The user can see all the tickets he bought, sorted by date of purchase.

### Note:

Services must communicate via http. For each task that the system has to process, if necessary, the services have to communicate with each other, one example would be to buy tickets, here service 3 must first communicate with service 2 to see if it is full capacity, then with service 1 to collect user data (card, discounts, etc.) and finally at the end of the purchase communicates again with service 2 to update the number of passengers for a given flight and then with service 1 to update that miles and rank of users.

Users must not contact the services directly, but there must be an api gateway between them that will deal with routing. Netflix libraries for api gateway (Zuul) and service discovery (Eureka).
