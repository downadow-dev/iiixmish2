package downadow.iiixmish2.main;

import java.awt.event.KeyEvent;
import javax.swing.*;
import java.awt.Color;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileWriter;

public class Iiixmish2 {
    static int[] ureg = new int[44], // пользовательские регистры
        treg = new int[8];           // регистры для потоков
    public static final byte
     NOP=    -1
    ,ADD=    -2
    ,SUB=    -3
    ,MOV=    -4
    ,ILD=    -5
    ,VLD=    -6
    ,LD=     -7
    ,ISV=    -8
    ,VSV=    -9
    ,SLP=    -10
    ,IFA=    -11
    ,IFB=    -12
    ,IFC=    -13
    ,IFD=    -14
    ,UPDD=   -15
    ,OFF=    -16
    ,VRST=   -17
    ,JMP=    -18
    ,END=    -19
    ,SEL=    -20
    ,MUL=    -21
    ,DIV=    -22
    ,LSLP=   -23
    ,CUR=    -24
    ,VSTR=   -25
    ,INC=    -26
    ,DEC=    -27
    ,TNP=    -28
    ,MOD=    -29
    ,EXP=    -30
    ,MOV2=   -31
    ,TH1=    -32
    ,TH2=    -33
    ,TH3=    -34
    ,MOV3=   -35
    ,VSVAN=  -36
    ,IFE=    -37
    ,LSHIFT= -38
    ,RSHIFT= -39
    ,XOR=    -40
    ,OR=     -41
    ,AND=    -42
    ;
    // память
    static int mem[] = new int[10000000];
    
    private static boolean th0, th1, th2, th3;
    
    private static boolean comm = false;
    
    static String progName = "";
    
    public static void main(String args[]) {
        if(args.length == 0 || args[0].startsWith("-")) {
            System.out.println("Использование:  java downadow.iiixmish2.main.Iiixmish2 ПУТЬ_К_ПРОГРАММЕ");
            System.out.println("                                                       [--no-communication]");
            System.exit(0);
        }
        try {
            progName = args[0];
            byte[] app = Files.readAllBytes(Paths.get(progName));
            if(app[0] != 0 || app[1] != 0x58 || app[2] != 0x4D || app[3] != 0x32) {
                System.err.println("iiixmish2: данный файл, похоже, не является программой для iiixmish2");
                System.exit(1);
            }
            for(int i = 4; i < app.length; i++) Iiixmish2.mem[i - 4] = app[i];
        } catch(Exception e) {}
        for(int i = 0; i < 2000; i++) {
            if(i < ureg.length) ureg[i] = 0;
            DISPLAY.ccells[i] = 0;
        }
        /* настройка экрана */
        DISPLAY.ccells[DISPLAY.ccells.length - 2] = 0; // параметр цвета для фона
        DISPLAY.ccells[DISPLAY.ccells.length - 1] = 1; // параметр цвета для ячеек видеопамяти
        DISPLAY.fr = new JFrame("iiixmish2");
        DISPLAY.fr.setIconImage(new ImageIcon("icon.png").getImage());
        DISPLAY.fr.setResizable(false);
        DISPLAY.fr.setSize(640, 480);
        DISPLAY.p.setLayout(null);
        DISPLAY.p.setBounds(0, 0, 640, 480);
        DISPLAY.fr.setLayout(null);
        DISPLAY.fr.setDefaultCloseOperation(3);
        DISPLAY.fr.setLocationRelativeTo(null);
        DISPLAY.fr.add(DISPLAY.p);
        DISPLAY.fr.addKeyListener(new java.awt.event.KeyListener() {
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_F1) {
                    for(int i = 0; i < treg.length; i++)
                        treg[i] = 0;
                    if(!th0) {
                        new Thread() {
                            public void run() {
                                memExec(0, 1);
                            }
                        }.start();
                    }
                    System.err.println("full reboot");
                } else if(e.getKeyCode() == KeyEvent.VK_F2) {
                    comm = true;
                    System.err.println("communication enabled");
                 } else if(e.getKeyCode() == KeyEvent.VK_F3) {
                    comm = false;
                    System.err.println("communication disabled");
                } else ureg[1] = e.getKeyChar();
            }
            public void keyReleased(KeyEvent e) {}
            public void keyTyped(KeyEvent e) {}
        });
        DISPLAY.fr.setVisible(true);
        DISPLAY.fr.repaint();
        if(args.length > 1 && !args[1].equals("--no-communication") || args.length == 1) {
            new Thread() {
                public void run() {
                    while(true) {
                        if(comm) {
                            try {
                                byte[] message = Files.readAllBytes(Paths.get(".comm"));
                                if(message.length < 1) {
                                    Thread.sleep(10);
                                    continue;
                                }
                                for(int i = 0; i < message.length; i++) {
                                    mem[9999872 + i] = (int)message[i];
                                }
                                Thread.sleep(10);
                                Files.deleteIfExists(Paths.get(".comm"));
                                Files.createFile(Paths.get(".comm"));
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            try { Thread.sleep(100); } catch(Exception e) {}
                        }
                    }
                }
            }.start();
            new Thread() {
                public void run() {
                    while(true) {
                        if(comm) {
                            try {
                                Thread.sleep(20);
                                Files.deleteIfExists(Paths.get(".comm2"));
                                Files.createFile(Paths.get(".comm2"));
                                byte[] answer = new byte[100];
                                for(int i = 0; i < answer.length; i++)
                                    answer[i] = (byte)mem[9999000 + i];
                                Files.write(Paths.get(".comm2"), answer);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            try { Thread.sleep(100); } catch(Exception e) {}
                        }
                    }
                }
            }.start();
        }
        memExec(0, 1);
    }
    static void memExec(int pcRA, int irRA) {
        if(pcRA == 0) th0 = true;
        else if(pcRA == 2) th1 = true;
        else if(pcRA == 4) th2 = true;
        else if(pcRA == 6) th3 = true;
        
        for(; treg[pcRA] < Iiixmish2.mem.length; treg[pcRA]++) {
            try {
                treg[irRA] = Iiixmish2.mem[treg[pcRA]];
                
                /* см. также <https://ru.wikibooks.org/Makexm2c> */
                
                if(treg[irRA] == ADD) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = ureg[Iiixmish2.mem[treg[pcRA] - 2]] + ureg[Iiixmish2.mem[treg[pcRA] - 3]];
                }
                else if(treg[irRA] == SUB) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = ureg[Iiixmish2.mem[treg[pcRA] - 2]] - ureg[Iiixmish2.mem[treg[pcRA] - 3]];
                }
                else if(treg[irRA] == MOV) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = Iiixmish2.mem[treg[pcRA] - 2];
                }
                else if(treg[irRA] == MOV2) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = Integer.parseInt((""+Iiixmish2.mem[treg[pcRA] - 2]+""+Iiixmish2.mem[treg[pcRA] - 3]+""+Iiixmish2.mem[treg[pcRA] - 4]+""+Iiixmish2.mem[treg[pcRA] - 5]+""+Iiixmish2.mem[treg[pcRA] - 6]+""+Iiixmish2.mem[treg[pcRA] - 7]));
                }
                else if(treg[irRA] == MOV3) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = Integer.parseInt((""+ureg[Iiixmish2.mem[treg[pcRA] - 2]]+""+ureg[Iiixmish2.mem[treg[pcRA] - 3]]+""+ureg[Iiixmish2.mem[treg[pcRA] - 4]]+""+ureg[Iiixmish2.mem[treg[pcRA] - 5]]+""+ureg[Iiixmish2.mem[treg[pcRA] - 6]]+""+ureg[Iiixmish2.mem[treg[pcRA] - 7]]));
                }
                else if(treg[irRA] == ISV) {
                     Iiixmish2.mem[Integer.parseInt(""+Iiixmish2.mem[treg[pcRA] - 1]+""+Iiixmish2.mem[treg[pcRA] - 2]+""+Iiixmish2.mem[treg[pcRA] - 3]+""+Iiixmish2.mem[treg[pcRA] - 4]+""+Iiixmish2.mem[treg[pcRA] - 5]+""+Iiixmish2.mem[treg[pcRA] - 6])] = ureg[Iiixmish2.mem[treg[pcRA] - 7]];
                }
                else if(treg[irRA] == VSVAN) {
                    for(int i = 0; i < ("" + ureg[Iiixmish2.mem[treg[pcRA] - 5]]).length(); i++) {
                        DISPLAY.ccells[Integer.parseInt(""+Iiixmish2.mem[treg[pcRA] - 1]+""+Iiixmish2.mem[treg[pcRA] - 2]+""+Iiixmish2.mem[treg[pcRA] - 3]+""+Iiixmish2.mem[treg[pcRA] - 4]) + i] = ("" + ureg[Iiixmish2.mem[treg[pcRA] - 5]]).toCharArray()[i];
                    }
                }
                else if(treg[irRA] == VSV) {
                     DISPLAY.ccells[Integer.parseInt(""+Iiixmish2.mem[treg[pcRA] - 1]+""+Iiixmish2.mem[treg[pcRA] - 2]+""+Iiixmish2.mem[treg[pcRA] - 3]+""+Iiixmish2.mem[treg[pcRA] - 4])] = (char)ureg[Iiixmish2.mem[treg[pcRA] - 5]];
                }
                else if(treg[irRA] == LD) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = ureg[Iiixmish2.mem[treg[pcRA] - 2]];
                }
                else if(treg[irRA] == ILD) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = Iiixmish2.mem[Integer.parseInt(""+Iiixmish2.mem[treg[pcRA] - 2]+""+Iiixmish2.mem[treg[pcRA] - 3]+""+Iiixmish2.mem[treg[pcRA] - 4]+""+Iiixmish2.mem[treg[pcRA] - 5]+""+Iiixmish2.mem[treg[pcRA] - 6]+""+Iiixmish2.mem[treg[pcRA] - 7])];
                }
                else if(treg[irRA] == VLD) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = DISPLAY.ccells[Integer.parseInt(""+Iiixmish2.mem[treg[pcRA] - 2]+""+Iiixmish2.mem[treg[pcRA] - 3]+""+Iiixmish2.mem[treg[pcRA] - 4]+""+Iiixmish2.mem[treg[pcRA] - 5])];
                }
                else if(treg[irRA] == SLP) {
                    Thread.sleep(ureg[Iiixmish2.mem[treg[pcRA] - 1]] * 1000);
                }
                else if(treg[irRA] == IFA && ureg[Iiixmish2.mem[treg[pcRA] - 1]] == ureg[Iiixmish2.mem[treg[pcRA] - 2]]) {
                    treg[pcRA] = ureg[Iiixmish2.mem[treg[pcRA] - 3]] - 1;
                } else if(treg[irRA] == IFB && ureg[Iiixmish2.mem[treg[pcRA] - 1]] != ureg[Iiixmish2.mem[treg[pcRA] - 2]]) {
                    treg[pcRA] = ureg[Iiixmish2.mem[treg[pcRA] - 3]] - 1;
                } else if(treg[irRA] == IFC && ureg[Iiixmish2.mem[treg[pcRA] - 1]] > ureg[Iiixmish2.mem[treg[pcRA] - 2]]) {
                    treg[pcRA] = ureg[Iiixmish2.mem[treg[pcRA] - 3]] - 1;
                } else if(treg[irRA] == IFD && ureg[Iiixmish2.mem[treg[pcRA] - 1]] < ureg[Iiixmish2.mem[treg[pcRA] - 2]]) {
                    treg[pcRA] = ureg[Iiixmish2.mem[treg[pcRA] - 3]] - 1;
                } else if(treg[irRA] == IFE && ureg[Iiixmish2.mem[treg[pcRA] - 1]] == ureg[Iiixmish2.mem[treg[pcRA] - 2]] && ureg[Iiixmish2.mem[treg[pcRA] - 3]] == ureg[Iiixmish2.mem[treg[pcRA] - 4]]) {
                    treg[pcRA] = ureg[Iiixmish2.mem[treg[pcRA] - 5]] - 1;
                }
                else if(treg[irRA] == UPDD) {
                    DISPLAY.fr.repaint();
                }
                else if(treg[irRA] == OFF) {
                    System.exit(0);
                }
                else if(treg[irRA] == VRST) {
                    ureg[pcRA + 5] = 0;
                    for(int i = 0; i < 1997; i++) {
                        DISPLAY.ccells[i] = (char)ureg[pcRA + 5];
                    }
                }
                else if(treg[irRA] == JMP) {
                    treg[pcRA] = ureg[Iiixmish2.mem[treg[pcRA] - 1]] - 1;
                }
                else if(treg[irRA] == END) {
                    break;
                }
                else if(treg[irRA] == SEL) {
                    /* случайности нет */
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = new java.util.Random().nextInt(ureg[Iiixmish2.mem[treg[pcRA] - 2]]);
                    /* случайности нет */
                }
                else if(treg[irRA] == MUL) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = ureg[Iiixmish2.mem[treg[pcRA] - 2]] * ureg[Iiixmish2.mem[treg[pcRA] - 3]];
                }
                else if(treg[irRA] == DIV) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = ureg[Iiixmish2.mem[treg[pcRA] - 2]] / ureg[Iiixmish2.mem[treg[pcRA] - 3]];
                }
                else if(treg[irRA] == LSLP) {
                    Thread.sleep(ureg[Iiixmish2.mem[treg[pcRA] - 1]]);
                }
                else if(treg[irRA] == CUR) {
                    Iiixmish2.mem[treg[pcRA]] = treg[pcRA];
                }
                else if(treg[irRA] == VSTR) {
                    for(int i = 0; i < Iiixmish2.mem[treg[pcRA] - 5]; i++) {
                        ureg[pcRA + 5] = (char)Iiixmish2.mem[treg[pcRA] - 6 - i];
                        DISPLAY.ccells[Integer.parseInt(""+Iiixmish2.mem[treg[pcRA] - 1]+""+Iiixmish2.mem[treg[pcRA] - 2]+""+Iiixmish2.mem[treg[pcRA] - 3]+""+Iiixmish2.mem[treg[pcRA] - 4]) + i] = (char)ureg[pcRA + 5];
                    }
                }
                else if(treg[irRA] == INC) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]]++;
                }
                else if(treg[irRA] == DEC) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]]--;
                }
                else if(treg[irRA] == TNP) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = -ureg[Iiixmish2.mem[treg[pcRA] - 1]];
                }
                else if(treg[irRA] == MOD) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = ureg[Iiixmish2.mem[treg[pcRA] - 2]] % ureg[Iiixmish2.mem[treg[pcRA] - 3]];
                }
                else if(treg[irRA] == EXP) {
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = (int)Math.pow(ureg[Iiixmish2.mem[treg[pcRA] - 2]], ureg[Iiixmish2.mem[treg[pcRA] - 3]]);
                }
                else if(treg[irRA] == TH1 && !th1) {
                    final int pcra = 2, irra = 3;
                    treg[pcra] = ureg[Iiixmish2.mem[treg[pcRA] - 1]];
                    new Thread() {
                        public void run() {
                            memExec(pcra, irra);
                        }
                    }.start();
                } else if(treg[irRA] == TH2 && !th2) {
                    final int pcra = 4, irra = 5;
                    treg[pcra] = ureg[Iiixmish2.mem[treg[pcRA] - 1]];
                    new Thread() {
                        public void run() {
                            memExec(pcra, irra);
                        }
                    }.start();
                } else if(treg[irRA] == TH3 && !th3) {
                    final int pcra = 6, irra = 7;
                    treg[pcra] = ureg[Iiixmish2.mem[treg[pcRA] - 1]];
                    new Thread() {
                        public void run() {
                            memExec(pcra, irra);
                        }
                    }.start();
                } else if(treg[irRA] == LSHIFT)
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = ureg[Iiixmish2.mem[treg[pcRA] - 2]] << ureg[Iiixmish2.mem[treg[pcRA] - 3]];
                else if(treg[irRA] == RSHIFT)
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = ureg[Iiixmish2.mem[treg[pcRA] - 2]] >> ureg[Iiixmish2.mem[treg[pcRA] - 3]];
                else if(treg[irRA] == XOR)
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = ureg[Iiixmish2.mem[treg[pcRA] - 2]] ^ ureg[Iiixmish2.mem[treg[pcRA] - 3]];
                else if(treg[irRA] == OR)
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = ureg[Iiixmish2.mem[treg[pcRA] - 2]] | ureg[Iiixmish2.mem[treg[pcRA] - 3]];
                else if(treg[irRA] == AND)
                    ureg[Iiixmish2.mem[treg[pcRA] - 1]] = ureg[Iiixmish2.mem[treg[pcRA] - 2]] & ureg[Iiixmish2.mem[treg[pcRA] - 3]];
            } catch(Exception e) {
                System.err.println("iiixmish2: PC = " + treg[pcRA] + " (treg " + pcRA + "),  " + e);
            }
        }
        
        if(pcRA == 0) th0 = false;
        else if(pcRA == 2) th1 = false;
        else if(pcRA == 4) th2 = false;
        else if(pcRA == 6) th3 = false;
    }
}
class DISPLAY extends JPanel {
    /* видеопамять */
    static char ccells[] = new char[2000];
    /* панель и рамка */
    static DISPLAY p = new DISPLAY();
    static JFrame fr;
    
    public void paint(java.awt.Graphics g) {
        /* выбрать цвет для фона */
        if(DISPLAY.ccells[ccells.length - 2] == 1)      g.setColor(new Color(255, 255, 255));
        else if(DISPLAY.ccells[ccells.length - 2] == 2) g.setColor(new Color(0, 255, 0));
        else if(DISPLAY.ccells[ccells.length - 2] == 3) g.setColor(new Color(0, 0, 255));
        else if(DISPLAY.ccells[ccells.length - 2] == 4) g.setColor(new Color(0, 120, 0));
        else if(DISPLAY.ccells[ccells.length - 2] == 5) g.setColor(new Color(120, 120, 120));
        else if(DISPLAY.ccells[ccells.length - 2] == 6) g.setColor(new Color(255, 0, 0));
        else if(DISPLAY.ccells[ccells.length - 2] == 7) g.setColor(new Color(255, 255, 0));
        else g.setColor(new Color(0, 0, 0));
        /* рисовать прямоугольник (фон) */
        g.drawRect(0, 0, 640, 480); g.fillRect(0, 0, 640, 480);
        /* выбрать шрифт для ячеек видеопамяти */
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14));
        /* рисование линий */
        int _ii = 0;
        for(int i = 0; i < 90; i += 5, _ii++) {
            if(DISPLAY.ccells[i + 1900] == 0) continue;
            else if(DISPLAY.ccells[i + 1900] == 1) g.setColor(new Color(255, 255, 255));
            else if(DISPLAY.ccells[i + 1900] == 2) g.setColor(new Color(0, 255, 0));
            else if(DISPLAY.ccells[i + 1900] == 3) g.setColor(new Color(0, 0, 255));
            else if(DISPLAY.ccells[i + 1900] == 4) g.setColor(new Color(0, 120, 0));
            else if(DISPLAY.ccells[i + 1900] == 5) g.setColor(new Color(120, 120, 120));
            else if(DISPLAY.ccells[i + 1900] == 6) g.setColor(new Color(255, 0, 0));
            else if(DISPLAY.ccells[i + 1900] == 7) g.setColor(new Color(255, 255, 0));
            else g.setColor(new Color(0, 0, 0));
            
            g.drawLine((int)ccells[i + 1901], (int)ccells[i + 1902], (int)ccells[i + 1903], (int)ccells[i + 1904]);
        }
        /* работа рисования ячеек видеопамяти */
        Iiixmish2.ureg[2] = 0;
        Iiixmish2.ureg[3] = 2;
        Iiixmish2.ureg[4] = 10;
        for(int i = 0; i < 30; i++) {
            if(i == 29)
                g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 13));
            for(int ii = 0; ii < 63; ii++) {
                /* выбрать цвет для ячеек видеопамяти */
                if(DISPLAY.ccells[ccells.length - 1] == 1)      g.setColor(new Color(255, 255, 255));
                else if(DISPLAY.ccells[ccells.length - 1] == 2) g.setColor(new Color(0, 255, 0));
                else if(DISPLAY.ccells[ccells.length - 1] == 3) g.setColor(new Color(0, 0, 255));
                else if(DISPLAY.ccells[ccells.length - 1] == 4) g.setColor(new Color(0, 120, 0));
                else if(DISPLAY.ccells[ccells.length - 1] == 5) g.setColor(new Color(120, 120, 120));
                else if(DISPLAY.ccells[ccells.length - 1] == 6) g.setColor(new Color(255, 0, 0));
                else if(DISPLAY.ccells[ccells.length - 1] == 7) g.setColor(new Color(255, 255, 0));
                else g.setColor(new Color(0, 0, 0));
                
                if(DISPLAY.ccells[(int)Iiixmish2.ureg[2]] < 191 && DISPLAY.ccells[(int)Iiixmish2.ureg[2]] > 0 ||
                   DISPLAY.ccells[(int)Iiixmish2.ureg[2]] > 1039 && DISPLAY.ccells[(int)Iiixmish2.ureg[2]] < 1106 ||
                   DISPLAY.ccells[(int)Iiixmish2.ureg[2]] > 9599 && DISPLAY.ccells[(int)Iiixmish2.ureg[2]] < 9621 ||
                   DISPLAY.ccells[(int)Iiixmish2.ureg[2]] == '—' || DISPLAY.ccells[(int)Iiixmish2.ureg[2]] == 'Ё')
                    g.drawString("" + DISPLAY.ccells[(int)Iiixmish2.ureg[2]], (int)Iiixmish2.ureg[3], (int)Iiixmish2.ureg[4]);
                else if(ccells[(int)Iiixmish2.ureg[2]] > 254 && ccells[(int)Iiixmish2.ureg[2]] < 270)
                    DISPLAY.ccells[ccells.length - 1] = (char)((int)ccells[(int)Iiixmish2.ureg[2]] - 255);
                
                Iiixmish2.ureg[2]++;
                Iiixmish2.ureg[3] += 10;
            }
            Iiixmish2.ureg[3] = 2;
            Iiixmish2.ureg[4] += 15;
        }
    }
    
}
