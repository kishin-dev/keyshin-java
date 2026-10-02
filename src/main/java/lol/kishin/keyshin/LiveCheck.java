package lol.kishin.keyshin;

public class LiveCheck {
    public static void main(String[] args) throws Exception {
        String url = "https://licenses.kishin.lol";
        String key = "KSSB-U8NU-D6PJ-BSAK-RX9L";
        String product = "kishinskyblockcore";

        System.out.println("validate() -> " + KeyShinClient.validate(url, key, product));

        ValidationResult r = KeyShinClient.check(url, key, product);
        System.out.println("valid:       " + r.valid());
        System.out.println("code:        " + r.code());
        System.out.println("message:     " + r.message());
        System.out.println("expiresAt:   " + r.expiresAt());
        System.out.println("activations: " + r.activations() + " / " + r.maxActivations());

        System.out.println("fake key  -> " + KeyShinClient.check(url, "FAKE-0000-0000-0000-0000", product).code());
        System.out.println("wrong prod-> " + KeyShinClient.check(url, key, "does-not-exist").code());
    }
}