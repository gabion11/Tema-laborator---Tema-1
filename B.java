import java.io.*;
import java.net.*;
import java.util.Scanner;

import static java.lang.Integer.parseInt;

public class B {
        // k_prim este o cheie de 128 de biti
        public static final String k_prim = "my secret keyaes";
        // vectorul de initializare
        public static final String initVector = "encryptionIntVec";

        // funtie pentru a afisa textul din binar in char
        public static void writeToFile(String[][] decryptBlocks, int numberOfBlocks){



            int caracter = 0;
            for (int i = 0; i < numberOfBlocks; i++) {
                for (int j = 0; j < 128; j++) {
                    if(j%8==0){

                        System.out.print((char)caracter);
                        caracter = (int) (Math.pow(2,7)*parseInt(decryptBlocks[i][j]));
                    }
                    else{
                        caracter+=Math.pow(2,7-j%8)*parseInt(decryptBlocks[i][j]);
                    }
                }
            }

        }
        // algoritmul de decriptare cbc
        public static String[][] CBCdecrypt(String[][] cryptoBloks,int numberOfBlocks,String key){

            String[][] plainBlocks1 = new String[numberOfBlocks][128];
            String[][] cryptoBloksCopy = cryptoBloks;

            String bitKey = A.stringToBinary(key);
            String bitIV = A.stringToBinary(A.initVector);

            for (int i = 0; i < numberOfBlocks; i++) {

                if (i==0){
                    for (int k = 0; k < 128; k++) {
                        plainBlocks1[i][k]= String.valueOf ((bitKey.charAt(k) ^ parseInt(cryptoBloks[i][k]))^bitIV.charAt(k));
                    }
                }
                else
                    for (int k = 0; k < 128; k++) {
                        plainBlocks1[i][k]= String.valueOf( (char)((parseInt(cryptoBloks[i][k])^bitKey.charAt(k))^parseInt(cryptoBloks[i-1][k])) );
                    }
            }
           writeToFile(plainBlocks1,numberOfBlocks);
            return plainBlocks1;
        }
        // algoritmul de decriptare ofb
        public static String[][] OFBdecrypt(String[][] cryptoBloks,int numberOfBlocks,String key){

        String[][] plainBlocks1 = new String[numberOfBlocks][128];
        String[][] keyXorPlain = new String[numberOfBlocks][128];
        String[][] cryptoBloksCopy = cryptoBloks;

        String bitKey = A.stringToBinary(key);
        String bitIV = A.stringToBinary(A.initVector);

            for (int i = 0; i < numberOfBlocks; i++) {
                if (i==0){
                    for (int k = 0; k < 128; k++) {
                        keyXorPlain[i][k] = String.valueOf(bitIV.charAt(k)^bitKey.charAt(k));
                        plainBlocks1[i][k] = String.valueOf(parseInt(keyXorPlain[i][k])^parseInt(cryptoBloks[i][k]));
                    }
                }
                else
                    for (int k = 0; k < 128; k++) {
                        keyXorPlain[i][k] = String.valueOf(parseInt(keyXorPlain[i-1][k])^bitKey.charAt(k));
                        plainBlocks1[i][k] = String.valueOf(parseInt(keyXorPlain[i][k])^parseInt(cryptoBloks[i][k]));
                    }

            }
        writeToFile(plainBlocks1,numberOfBlocks);
        return plainBlocks1;
    }

        public static void main(String[] args) {
            try{
                Socket s=new Socket("localhost",6666);
                Scanner scanner = new Scanner(System.in);
                DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                DataInputStream dis = new DataInputStream(s.getInputStream());
                String str ="";
                while (str != "bye") {

                    str = (String) dis.readUTF();
                    System.out.println("modul= " + str);
                    String modul = str;
                    if(str.compareTo("CBC")==0|| str.compareTo("OFB")==0) {
                        // citim cheia de la nodul A
                        String Key = (String) dis.readUTF();
                        System.out.println("Key= "+Key);

                        //decryptam cheia primita
                        String decryptedKey = KM.decrypt(Key,k_prim,initVector);
                        System.out.println("decryptedKey= "+decryptedKey);

                        // trimitem nodului  A mesaj ca putem incepe comunicare
                        dout.writeUTF("OK");

                        // primim textul criptat de la nodul A
                        str =  dis.readUTF();
                        String[][] cryptoBloks = new String[parseInt(str)][128];

                        for (int i = 0; i <parseInt(str); i++) {
                            for (int j = 0; j < 128; j++) {
                                cryptoBloks[i][j] = dis.readUTF();
                            }
                        }
                        System.out.println("mesajul criptat");
                        writeToFile(cryptoBloks,parseInt(str));
                        System.out.println();
                        System.out.println();
                        System.out.println("mesajul decryptat");
                        // in functie de modul primit decriptam textul
                        if (modul.compareTo("CBC")==0)
                            CBCdecrypt(cryptoBloks,parseInt(str),decryptedKey);
                        else
                            OFBdecrypt(cryptoBloks,parseInt(str),decryptedKey);
                    }


                    String inputString = scanner.nextLine();

                    dout.writeUTF(inputString);

                    dout.flush();


                }
                s.close();
            }catch(Exception e){System.out.println(e);}
        }
    }

