data class GraphQl(

    var query: String = "{\n" +
            "  viewer {\n" +
            "    homes {\n" +
            "      currentSubscription {\n" +
            "        priceInfo {\n" +
            "          current {\n" +
            "            currency\n" +
            "            total\n" +
            "            energy\n" +
            "            tax\n" +
            "            startsAt\n" +
            "          }\n" +
            "          today {\n" +
            "            total\n" +
            "            energy\n" +
            "            tax\n" +
            "            startsAt\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n",
    var variables: String? = null,
    var operationName: String? = null

)