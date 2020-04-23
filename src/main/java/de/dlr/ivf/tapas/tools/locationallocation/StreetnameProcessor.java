package de.dlr.ivf.tapas.tools.locationallocation;


import java.util.ArrayList;
import java.util.List;

public class StreetnameProcessor {

    /**
     * The List for the street tags
     */
    private final List<StreetTagEntry> streetTagList = new ArrayList<>();
    /**
     * The street name. Input and output.
     */
    private String street = null;
    /**
     * The street number. Input and output.
     */
    private String streetNumber = null;
    /**
     * The zip code. Input and output.
     */
    private String zipCode = null;
    /**
     * The city. Input and output.
     */
    private String city = null;
    /**
     * The input string. Input.
     */
    private String inputString = null;
    /**
     * Default constructor. fills the street tag list.
     */
    public StreetnameProcessor() {
        this.streetTagList.add(new StreetTagEntry("lt-Rudow"));
        this.streetTagList.add(new StreetTagEntry("lt-Buckow"));
        this.streetTagList.add(new StreetTagEntry("lt-Lichtenrade"));
        this.streetTagList.add(new StreetTagEntry("lt-Lankwitz"));
        this.streetTagList.add(new StreetTagEntry("lt-Mariendorf"));
        this.streetTagList.add(new StreetTagEntry("lt-Tempelhof"));
        this.streetTagList.add(new StreetTagEntry("lt-Friedrichsfelde"));
        this.streetTagList.add(new StreetTagEntry("lt-Tegel"));
        this.streetTagList.add(new StreetTagEntry("lt-Moabit"));
        this.streetTagList.add(new StreetTagEntry("lt-Lietzow"));
        this.streetTagList.add(new StreetTagEntry("park Wilmersdorf"));
        this.streetTagList.add(new StreetTagEntry("lt-Köpenick"));
        this.streetTagList.add(new StreetTagEntry("m Flugplatz Gatow"));
        this.streetTagList.add(new StreetTagEntry("llee der Kosmonauten"));
        this.streetTagList.add(new StreetTagEntry("park Friedrichshain"));
        this.streetTagList.add(new StreetTagEntry("Straße"));
        this.streetTagList.add(new StreetTagEntry("Str.", "Straße"));
        this.streetTagList.add(new StreetTagEntry(" Str", "Straße"));
        this.streetTagList.add(new StreetTagEntry("STR.", "Straße"));
        this.streetTagList.add(new StreetTagEntry("Strasse", "Straße"));
        this.streetTagList.add(new StreetTagEntry("strasse", "straße"));
        this.streetTagList.add(new StreetTagEntry("straße"));
        this.streetTagList.add(new StreetTagEntry("str ", "straße"));
        this.streetTagList.add(new StreetTagEntry("str.", "straße"));
        this.streetTagList.add(new StreetTagEntry("Allee"));
        this.streetTagList.add(new StreetTagEntry("Al.", "Allee"));
        this.streetTagList.add(new StreetTagEntry("allee"));
        this.streetTagList.add(new StreetTagEntry("al.", "allee"));
        this.streetTagList.add(new StreetTagEntry("Weg"));
        this.streetTagList.add(new StreetTagEntry("Wg.", "Weg"));
        this.streetTagList.add(new StreetTagEntry("weg"));
        this.streetTagList.add(new StreetTagEntry("wg.", "weg"));
        this.streetTagList.add(new StreetTagEntry("Damm"));
        this.streetTagList.add(new StreetTagEntry("Dm.", "Damm"));
        this.streetTagList.add(new StreetTagEntry("damm"));
        this.streetTagList.add(new StreetTagEntry("dm.", "damm"));
        this.streetTagList.add(new StreetTagEntry("Promenade"));
        this.streetTagList.add(new StreetTagEntry("promenade"));
        this.streetTagList.add(new StreetTagEntry("Pfad"));
        this.streetTagList.add(new StreetTagEntry("pfad"));
        this.streetTagList.add(new StreetTagEntry("Gasse"));
        this.streetTagList.add(new StreetTagEntry("gasse"));
        this.streetTagList.add(new StreetTagEntry("Platz"));
        this.streetTagList.add(new StreetTagEntry("Pl.", "Platz"));
        this.streetTagList.add(new StreetTagEntry("platz"));
        this.streetTagList.add(new StreetTagEntry("pl.", "platz"));
        this.streetTagList.add(new StreetTagEntry("Zeile"));
        this.streetTagList.add(new StreetTagEntry("zeile"));
        this.streetTagList.add(new StreetTagEntry("Chaussee"));
        this.streetTagList.add(new StreetTagEntry("Ch.", "Chaussee"));
        this.streetTagList.add(new StreetTagEntry("chaussee"));
        this.streetTagList.add(new StreetTagEntry("ch.", "chaussee"));
        this.streetTagList.add(new StreetTagEntry("Ring"));
        this.streetTagList.add(new StreetTagEntry("ring"));
        this.streetTagList.add(new StreetTagEntry("korso"));
        this.streetTagList.add(new StreetTagEntry("turm"));
        this.streetTagList.add(new StreetTagEntry("Park"));
        this.streetTagList.add(new StreetTagEntry("park"));
        this.streetTagList.add(new StreetTagEntry("Ufer"));
        this.streetTagList.add(new StreetTagEntry("ufer"));
        this.streetTagList.add(new StreetTagEntry("Steig"));
        this.streetTagList.add(new StreetTagEntry("steig"));
        this.streetTagList.add(new StreetTagEntry("All", "Allee"));
        this.streetTagList.add(new StreetTagEntry("Straß", "Straße"));
        this.streetTagList.add(new StreetTagEntry("Str", "Straße"));
        this.streetTagList.add(new StreetTagEntry("str", "straße"));

    }

    private String capStupidAmendments(String number) {
        String returnString = "";
        if (number != null && number.length() > 0) {
            number = number.replaceAll(" ", ""); //remove spaces
            boolean advanceToNextPos = true;
            int pos = 0;
            while (advanceToNextPos) {
                switch (number.charAt(pos)) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                    case '-':
                        //a number! yeh!
                        if (pos + 1 == number.length()) advanceToNextPos = false;
                        else pos++;
                        break;
                    default:
                        advanceToNextPos = false;
                        break;
                }
            }
            if (pos + 1 == number.length() || pos == number.length()) returnString = number;
            else returnString = number.substring(0, pos).trim();
        }
        return returnString;
    }

    //characters allowed after Street tag: null, Space and 0-9
    private boolean checkCharacterAfterTag(String input, String tag) {
        if (input.startsWith(tag)) {
            if (input.length() == tag.length()) // nothing behind the tag
                return true;
            char c = input.charAt(tag.length());
            //legal character?
            return c == ' ' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' ||
                    c == '8' || c == '9' || c == '0';
        } else {
            return false;
        }
    }

    /**
     * This method checks if the input string contains a street tag. Search starts from the right!
     *
     * @param input The String to test
     * @return The found street tag or null if no tag is found.
     */
    public StreetTagEntry containsTag(String input) {
        if (input == null) return null;
        this.inputString = input;
        String tmp, tag;
        int i;
        for (StreetTagEntry e : this.streetTagList) {
            tag = e.getOriginalTag();
            //from end-taglength to pos 1
            for (i = this.inputString.length() - tag.length(); i > 0; --i) {
                tmp = this.inputString.substring(i);
                if (checkCharacterAfterTag(tmp, tag)) {
                    return e;
                }
            }
        }
        return null;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getStreetNumber() {
        return streetNumber;
    }

    public void setStreetNumber(String streetNumber) {
        this.streetNumber = streetNumber;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public boolean processStreet(String input) {

        StreetTagEntry tag = this.containsTag(input);
        if (tag != null) {

            this.zipCode = null;
            this.streetNumber = null;
            if (inputString.indexOf(tag.getOriginalTag()) ==
                    0) //oh eine "Straße des 17. Juni" oder "Allee der Kosmonauten"
                return false;
            //alles bis incl. tag ist Staßenname
            this.setStreet(replaceStreetTag(inputString, tag));

            if (inputString.indexOf(tag.getOriginalTag()) + tag.getOriginalTag().length() + 1 < inputString.length()) {
                //this is the rest
                inputString = inputString.substring(
                        inputString.indexOf(tag.getOriginalTag()) + tag.getOriginalTag().length() + 1).trim();

                //do some replaces
                if (inputString.startsWith("SP")) //Spielplätze
                    inputString = "1";
                inputString = inputString.replace("IV", "4"); //Römische zahl
                inputString = inputString.replace("V", "5"); //Römische zahl
                inputString = inputString.replace("III", "3"); //Römische zahl
                inputString = inputString.replace("II", "2"); //Römische zahl
                inputString = inputString.replace("I", "1"); //Römische zahl

                //check if we have a comma separated houseNumber
                if (inputString.indexOf(",") > 0) {
                    this.streetNumber = inputString.substring(0, inputString.indexOf(",")).trim();
                    inputString = inputString.substring(inputString.indexOf(",") + 1).trim();
                } else if (inputString.indexOf(" ") > 0) {
                    this.streetNumber = inputString.substring(0, inputString.indexOf(" ")).trim();
                    inputString = inputString.substring(inputString.indexOf(" ") + 1).trim();
                } else {
                    this.streetNumber = inputString;
                    inputString = "";
                }

                if (this.city != null) {
                    if (inputString.indexOf(this.city) > 0) {
                        inputString = inputString.substring(0, inputString.indexOf(this.city)).trim();
                    }
                }
                if (inputString.length() == 5) { //PLZ!
                    this.zipCode = inputString;
                    inputString = "";
                }
                this.streetNumber = capStupidAmendments(this.streetNumber);
            } else {
                //only a streetname: set housenumber to 1
                this.streetNumber = null;
                //if (Constants.ENABLE_CHANGES_OF_MARCO)
                streetNumber = "1";
            }
            if (this.streetNumber != null && this.streetNumber.length() > 0) {
                switch (this.streetNumber.charAt(0)) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        //a number! yeh!
                        return true;
                    default:
                        return false;
                }
            } else return false;
        } else {
            //check for simple format: street+number
            int pos = input.indexOf(" ");
            if (pos >= 0) {
                String numberToCheck = input.substring(pos + 1).trim();
                if (numberToCheck.length() > 1) {
                    switch (numberToCheck.charAt(numberToCheck.length() - 1)) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            //a number! yeh!
                            this.street = input.substring(0, pos).trim();
                            this.streetNumber = capStupidAmendments(numberToCheck);
                            break;
                        default:
                            return false;
                    }
                }

                //check first char
                switch (numberToCheck.charAt(0)) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        //a number! yeh!
                        this.street = input.substring(0, pos).trim();
                        this.streetNumber = capStupidAmendments(numberToCheck);
                        return true;
                    default:
                        return false;
                }
            } else return false;
        }
    }

    public String replaceStreetTag(String input) {
        StreetTagEntry tag = this.containsTag(input);
        return replaceStreetTag(input, tag);
    }

    private String replaceStreetTag(String input, StreetTagEntry tag) {
        String result = input;
        if (tag != null) {

            if (inputString.indexOf(tag.getOriginalTag()) !=
                    0) { //keine "Straße des 17. Juni" oder "Allee der Kosmonauten"
                //alles bis incl. tag ist Staßenname
                result = ((inputString.substring(0, inputString.indexOf(tag.getOriginalTag())) + tag.getReplaceTag())
                        .trim());
            }
        }
        return result;
    }

    class StreetTagEntry {
        private final String originalTag;
        private final String replaceTag;

        StreetTagEntry(String originalTagS, String replaceTagS) {
            originalTag = originalTagS;
            replaceTag = replaceTagS;
        }

        StreetTagEntry(String originalTagS) {
            originalTag = originalTagS;
            replaceTag = originalTagS;
        }

        /**
         * @return the originalTag
         */
        public String getOriginalTag() {
            return originalTag;
        }

        /**
         * @return the replaceTag
         */
        public String getReplaceTag() {
            return replaceTag;
        }
    }


}
