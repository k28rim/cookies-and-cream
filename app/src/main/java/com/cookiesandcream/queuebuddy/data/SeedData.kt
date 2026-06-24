package com.cookiesandcream.queuebuddy.data

import com.cookiesandcream.queuebuddy.domain.model.CampusLocation
import com.cookiesandcream.queuebuddy.domain.model.LocationCategory

// The campus locations the app ships with (20 real UW spots).
object SeedData {

    val locations: List<CampusLocation> = listOf(

        CampusLocation(
            id = "food-dc-cafeteria",
            name = "DC Cafeteria (Tim Hortons)",
            building = "Davis Centre",
            category = LocationCategory.FOOD,
            description = "Tim Hortons and grab-and-go food in the Davis Centre. Busiest between classes.",
            latitude = 43.47273, longitude = -80.54200,
            amenities = listOf("Debit/credit", "WatCard accepted", "Seating nearby")
        ),
        CampusLocation(
            id = "food-eng-cnd",
            name = "Engineering C&D",
            building = "CPH",
            category = LocationCategory.FOOD,
            description = "Student-run coffee and donut shop with snacks, sandwiches, and cheap coffee.",
            latitude = 43.47092, longitude = -80.53963,
            amenities = listOf("Cash & WatCard", "Cheap coffee")
        ),
        CampusLocation(
            id = "food-slc-foodcourt",
            name = "SLC Food Court",
            building = "Student Life Centre",
            category = LocationCategory.FOOD,
            description = "Multiple vendors in the Student Life Centre lower level.",
            latitude = 43.47163, longitude = -80.54565,
            amenities = listOf("Many vendors", "Microwaves", "Lots of seating")
        ),
        CampusLocation(
            id = "food-ml-cnd",
            name = "ML C&D (Arts Coffee Shop)",
            building = "Modern Languages",
            category = LocationCategory.FOOD,
            description = "Arts student coffee and donut shop, usually a shorter line than the SLC.",
            latitude = 43.46907, longitude = -80.54225,
            amenities = listOf("Cash & WatCard", "Baked goods")
        ),
        CampusLocation(
            id = "food-v1-cafeteria",
            name = "V1 Cafeteria (Mudie's)",
            building = "Village 1",
            category = LocationCategory.FOOD,
            description = "Residence cafeteria open to everyone, with hot meals all day.",
            latitude = 43.47155, longitude = -80.55095,
            amenities = listOf("Hot meals", "Meal plan accepted")
        ),

        CampusLocation(
            id = "study-slc-greathall",
            name = "SLC Great Hall",
            building = "Student Life Centre",
            category = LocationCategory.STUDY,
            description = "Open social study space in the heart of campus. Lively, rarely quiet.",
            latitude = 43.47163, longitude = -80.54565,
            amenities = listOf("Outlets available", "Open 24/7", "Group friendly")
        ),
        CampusLocation(
            id = "study-e7-lounge",
            name = "E7 Study Lounges",
            building = "Engineering 7",
            category = LocationCategory.STUDY,
            description = "Modern study pods and open tables across the upper floors of E7.",
            latitude = 43.47298, longitude = -80.53963,
            amenities = listOf("Outlets available", "Whiteboards", "Standing desks")
        ),
        CampusLocation(
            id = "study-qnc-atrium",
            name = "QNC Study Atrium",
            building = "Quantum-Nano Centre",
            category = LocationCategory.STUDY,
            description = "Bright atrium seating with tables and benches, moderate noise.",
            latitude = 43.47105, longitude = -80.54401,
            amenities = listOf("Outlets available", "Natural light")
        ),
        CampusLocation(
            id = "study-mc-comfy",
            name = "MC Comfy Lounge",
            building = "Mathematics & Computer",
            category = LocationCategory.STUDY,
            description = "Math student lounge with couches and tables on the 3rd floor of MC.",
            latitude = 43.47207, longitude = -80.54399,
            amenities = listOf("Couches", "Microwave nearby")
        ),

        CampusLocation(
            id = "lib-dp-6th",
            name = "Dana Porter Library (6th floor)",
            building = "Dana Porter",
            category = LocationCategory.LIBRARY,
            description = "Silent individual study floor with single desks — the classic quiet spot.",
            latitude = 43.46971, longitude = -80.54243,
            amenities = listOf("Silent floor", "Individual desks", "Outlets available")
        ),
        CampusLocation(
            id = "lib-dc-library",
            name = "Davis Centre Library",
            building = "Davis Centre",
            category = LocationCategory.LIBRARY,
            description = "STEM library with quiet study carrels and bookable group rooms.",
            latitude = 43.47273, longitude = -80.54200,
            amenities = listOf("Quiet zones", "Group rooms", "Outlets available")
        ),

        CampusLocation(
            id = "gym-pac",
            name = "PAC Fitness Centre",
            building = "Physical Activities Complex",
            category = LocationCategory.GYM,
            description = "Main campus weight room and cardio equipment.",
            latitude = 43.47242, longitude = -80.54603,
            amenities = listOf("Weights", "Cardio machines", "Change rooms")
        ),
        CampusLocation(
            id = "gym-cif",
            name = "CIF Fitness Centre",
            building = "Columbia Icefield",
            category = LocationCategory.GYM,
            description = "North campus gym, usually quieter than PAC outside peak hours.",
            latitude = 43.47554, longitude = -80.54793,
            amenities = listOf("Weights", "Track nearby")
        ),

        CampusLocation(
            id = "print-dc-library",
            name = "DC Library Printers",
            building = "Davis Centre",
            category = LocationCategory.PRINTER,
            description = "W Print stations inside the Davis Centre library.",
            latitude = 43.47273, longitude = -80.54200,
            amenities = listOf("W Print", "Colour printing", "Scanner")
        ),
        CampusLocation(
            id = "print-dp-library",
            name = "Dana Porter Printers",
            building = "Dana Porter",
            category = LocationCategory.PRINTER,
            description = "W Print stations on the main floor of Dana Porter library.",
            latitude = 43.46971, longitude = -80.54243,
            amenities = listOf("W Print", "Scanner")
        ),
        CampusLocation(
            id = "print-mc",
            name = "MC Printer Room",
            building = "Mathematics & Computer",
            category = LocationCategory.PRINTER,
            description = "Printers near the MC labs, handy before math and CS lectures.",
            latitude = 43.47207, longitude = -80.54399,
            amenities = listOf("W Print")
        ),
        CampusLocation(
            id = "print-slc",
            name = "SLC Print Station",
            building = "Student Life Centre",
            category = LocationCategory.PRINTER,
            description = "Print station near Turnkey Desk, open long hours.",
            latitude = 43.47163, longitude = -80.54565,
            amenities = listOf("W Print", "Open late")
        ),

        CampusLocation(
            id = "svc-turnkey",
            name = "Turnkey Desk",
            building = "Student Life Centre",
            category = LocationCategory.SERVICE,
            description = "24/7 info desk: tickets, lost and found, and general help.",
            latitude = 43.47163, longitude = -80.54565,
            amenities = listOf("Open 24/7")
        ),
        CampusLocation(
            id = "svc-tcard",
            name = "WatCard Office",
            building = "Needles Hall",
            category = LocationCategory.SERVICE,
            description = "WatCard pickup and reload services. Lines spike at term start.",
            latitude = 43.46963, longitude = -80.54462,
            amenities = listOf("Card services")
        ),
        CampusLocation(
            id = "svc-registrar",
            name = "The Centre (Registrar)",
            building = "Needles Hall",
            category = LocationCategory.SERVICE,
            description = "Registrar service desk for transcripts, enrolment letters, and forms.",
            latitude = 43.46963, longitude = -80.54462,
            amenities = listOf("Student records")
        )
    )
}
