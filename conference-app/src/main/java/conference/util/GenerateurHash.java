package conference.util;

import org.mindrot.jbcrypt.BCrypt;
import java.util.Scanner;

public class GenerateurHash {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== GÉNÉRATEUR DE HASH BCRYPT ===");
        System.out.println("Entrez 'quit' pour quitter\n");

        while (true) {
            System.out.print("Mot de passe à hasher: ");
            String password = scanner.nextLine();

            if (password.equalsIgnoreCase("quit")) {
                System.out.println("Au revoir !");
                break;
            }

            if (password.isEmpty()) {
                System.out.println("Veuillez entrer un mot de passe valide.\n");
                continue;
            }

            String hash = BCrypt.hashpw(password, BCrypt.gensalt(10));

            System.out.println("\n--- RÉSULTAT ---");
            System.out.println("Mot de passe : " + password);
            System.out.println("Hash         : " + hash);
            System.out.println("\n📋 Requête SQL à exécuter :");
            System.out.println("UPDATE utilisateurs SET mot_de_passe = '" + hash + "' WHERE email = 'utilisateur@email.com';");
            System.out.println("\n-----------------------------------\n");
        }

        scanner.close();
    }
}