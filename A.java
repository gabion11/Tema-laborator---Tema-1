import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import static java.lang.Integer.parseInt;

public class A {
        // k_prim este o cheie de 128 de biti
        public static final String k_prim = "my secret keyaes";
        // vectorul de initializare
        public static final String initVector = "encryptionIntVec";
        // funtie pentru a transforma un string in binar
        public static String stringToBinary(String str){
            String temps;
            String binaryStr = "";
            for(int i = 0; i < str.length(); i++){

                temps = Integer.toBinaryString(str.charAt(i));

                while(temps.length() < 8){
                    temps = "0" + temps;
                    if(temps.length() < 8)
                        temps = "0" + temps;
                }
                binaryStr = binaryStr + temps;
            }
            return binaryStr;
        }
        // functie pentru a transforma un strin in blocuri de 128 biti
        public static String[][] splitStr(String str,int numberOfBlocks){
            String[][] plainBlocks = new String[numberOfBlocks][128];

            for (int i = 0; i<numberOfBlocks-1;i++)
                for (int j = 0; j <128 ; j++) {
                    plainBlocks[i][j] = String.valueOf(str.charAt(j+(128*i)));
                }
            int j=0;
            for (int i = 0; i < 128; i++) {
                plainBlocks[numberOfBlocks-1][i]= String.valueOf(0);
            }
            for (int i = (numberOfBlocks-1)*128; i <str.length() ; i++) {
                plainBlocks[(numberOfBlocks-1)][j] = String.valueOf(str.charAt(i));
                j++;
            }
            return plainBlocks;
        }
        // algoritmul cbc care cripteaza un string
        public static String[][] CBC(String text, String key){
            int numberOfBlocks = text.length()/128+1;

            String[][] cryptoBloks = new String[numberOfBlocks][128];

            //inpartim plaintextul in blocuri de 128 de biti
            String[][] plainBlocks = splitStr(text,numberOfBlocks);


            // incepem sa criptam
            String bitKey = A.stringToBinary(key);
            String bitIV = A.stringToBinary(A.initVector);

            for (int i = 0; i < numberOfBlocks; i++) {
                if (i==0){
                    for (int k = 0; k < 128; k++) {
                        cryptoBloks[i][k]= String.valueOf ((bitIV.charAt(k) ^ parseInt(plainBlocks[i][k]))^bitKey.charAt(k));
                    }
                }
                else
                    for (int k = 0; k < 128; k++) {
                        cryptoBloks[i][k]= String.valueOf( (char)((parseInt(cryptoBloks[i-1][k])^ parseInt(plainBlocks[i][k]))^bitKey.charAt(k)) );
                    }
            }

            return cryptoBloks;
        }
        // algoritmul ofb care cripteaza un string
        public static String[][] OFB(String text, String key){
            int numberOfBlocks = text.length()/128+1;

            String[][] cryptoBloks = new String[numberOfBlocks][128];
            String[][] keyXorPlain = new String[numberOfBlocks][128];
            //inpartim plaintextul in blocuri de 128 de biti
            String[][] plainBlocks = splitStr(text,numberOfBlocks);


            // incepem sa criptam
            String bitKey = A.stringToBinary(key);
            String bitIV = A.stringToBinary(A.initVector);

            for (int i = 0; i < numberOfBlocks; i++) {
                if (i==0){
                    for (int k = 0; k < 128; k++) {
                        keyXorPlain[i][k] = String.valueOf(bitIV.charAt(k)^bitKey.charAt(k));
                        cryptoBloks[i][k] = String.valueOf(parseInt(keyXorPlain[i][k])^parseInt(plainBlocks[i][k]));
                    }
                }
                else
                    for (int k = 0; k < 128; k++) {
                        keyXorPlain[i][k] = String.valueOf(parseInt(keyXorPlain[i-1][k])^bitKey.charAt(k));
                        cryptoBloks[i][k] = String.valueOf(parseInt(keyXorPlain[i][k])^parseInt(plainBlocks[i][k]));
                    }

            }

            return cryptoBloks;
        }

        public static void main(String[] args){
            try{
                ServerSocket ss=new ServerSocket(6666);
                Socket s=ss.accept();//establishes connection

                Scanner scanner = new Scanner(System.in);
                DataInputStream dis = new DataInputStream(s.getInputStream());
                DataOutputStream dout=new DataOutputStream(s.getOutputStream());
                String str = "";

                while(str != "bye") {
                    System.out.println("introduceti un mod de comunicare CBC sau OFB");
                    String inputString = scanner.nextLine();
                    String modul = inputString;
                    if(inputString.compareTo("CBC")==0|| inputString.compareTo("OFB")==0) {
                        //trimitem nodului B modul de operare
                        dout.writeUTF(inputString);

                        // folosim nodul KM pentru a genera K
                        String Key= KM.encrypt(KM.key_generator().toString(),initVector,k_prim);

                        //am generat cheia K iar acum o timitem nodului B
                        System.out.println("Key= "+ Key);
                        dout.writeUTF(Key);

                        // decriptam cheia cu o  functie din nodul KM
                        String decryptedKey = KM.decrypt(Key,k_prim,initVector);
                        System.out.println("decryptedKey= "+decryptedKey);
                        //astemtam confirmare de la nodul B
                        str = (String) dis.readUTF();

                        if(str.compareTo("OK")==0 )
                        {
                            // incepem comunicarea
                            System.out.println("incepem comunicare");

                            //deschidem fisierul pe care vrem sa il trimitem
                            Path path = Paths.get("D:\\Anul III\\SI\\tema1_java\\src\\text.txt");
                            File file = new File(String.valueOf(path));
                            FileInputStream fis = new FileInputStream(file);
                            byte[] data = new byte[(int) file.length()];
                            fis.read(data);
                            fis.close();
                            System.out.println("mesajul pe care vrem sa il criptam");
                            System.out.println(new String(data,"UTF-8"));
                            // transforma textul in binat
                            String plaintext = A.stringToBinary(new String(data,"UTF-8"));

                            int numberOfBlocks = plaintext.length()/128+1;
                            String[][] cryptoBloks;

                            if(modul.compareTo("CBC")==0){

                                cryptoBloks = CBC(plaintext,decryptedKey);
                            }
                            else
                                cryptoBloks = OFB(plaintext,decryptedKey);

                            //trimitem mesajul catre nodul B

                            dout.writeUTF(String.valueOf(numberOfBlocks));
                            for (int i = 0; i < numberOfBlocks; i++) {
                                for (int j = 0; j < 128; j++) {
                                    dout.writeUTF(cryptoBloks[i][j]);
                                }
                            }
                            B.writeToFile(cryptoBloks,numberOfBlocks);


                        }
                        System.out.println();
                        System.out.println("message= " + str);
                    }
                    else
                        System.out.println("invalid command");


                }
                ss.close();

            }catch(Exception e){System.out.println(e);}
        }
    }

