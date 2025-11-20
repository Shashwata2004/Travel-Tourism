so basically this model package copies db data in java format. the services access data from there. and then it give the data to controllers via http request form frontend.

JSON first hits Controller
👉 JSON becomes a DTO
👉 Controller sends DTO to Service
👉 Service uses Entities + DB
👉 Service returns result
👉 Controller sends JSON back to frontend.