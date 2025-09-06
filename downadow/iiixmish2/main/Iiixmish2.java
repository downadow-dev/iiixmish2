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
    static int[] ureg = new int[28];  // регистры
    static int pc = 1;
    public static final byte
     NOP=    1
    ,ADD=    2
    ,SUB=    3
    ,MOV=    4
    ,ILD=    5
    ,OPEN=   6
    ,ISV=    7
    ,CALL=   8
    ,IFA=    9
    ,IFB=    10
    ,IFC=    11
    ,IFD=    12
    ,OFF=    13
    ,JMP=    14
    ,MUL=    15
    ,DIV=    16
    ,INC=    17
    ,DEC=    18
    ,TNP=    19
    ,MOD=    20
    ,LSHIFT= 21
    ,RSHIFT= 22
    ,XOR=    23
    ,OR=     24
    ,AND=    25
    ,TIME=   26
    ,TRST=   27
    ,RISV=   28
    ,RILD=   29
    ,RVSV=   30
    ,RVLD=   31
    ,VSV=    32
    ,VLD=    33
    ;
    
    /* память
       ======
       код вне 0-(mem[0]-1) не может читать/прыгать/писать в 0-(mem[0]-1),
       переход к 512 возможен с OFF;
       код вне 0-(mem[0]-1) не может использовать инструкцию OPEN;
     */
    static int mem[] = new int[10000000];
    
    /* видеопамять */
    static int vmem[] = new int[2000];
    
    static int currentPort = -1;
    
    private static long startTime;
    
    public static void main(String args[]) {
        if(args.length == 0) {
            System.out.println("Использование:  java downadow.iiixmish2.main.Iiixmish2 ПУТЬ_К_ОБРАЗУ");
            System.exit(0);
        }
        try {
            Scanner sc = new Scanner(new File(args[0]));
            for(int i = 0; sc.hasNextLine(); i++)
                mem[i] = Integer.parseInt(sc.nextLine());
            sc.close();
        } catch(Exception e) {}
        for(int i = 0; i < vmem.length; i++) {
            if(i < ureg.length) ureg[i] = 0;
            vmem[i] = 0;
        }
        /* настройка экрана */
        Iiixmish2.vmem[1998] = 0; // параметр цвета для фона
        Iiixmish2.vmem[1999] = 1; // параметр цвета для ячеек видеопамяти
        DISPLAY.fr = new JFrame("iiixmish2");
        DISPLAY.fr.setResizable(false);
        DISPLAY.fr.setSize(572, 576);
        DISPLAY.p.setLayout(null);
        DISPLAY.p.setBounds(0, 0, 572, 576);
        DISPLAY.fr.setLayout(null);
        DISPLAY.fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        DISPLAY.fr.setLocationRelativeTo(null);
        DISPLAY.fr.add(DISPLAY.p);
        DISPLAY.fr.addKeyListener(new java.awt.event.KeyListener() {
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_F1) {
                    System.out.println("===============================");
                    
                    System.out.println("PC: " + pc);
                    pc = mem.length;
                    System.out.println("Registers:");
                    for(int i = 0; i < ureg.length; i++)
                        System.out.println("\t" + i + "\t" + ureg[i]);
                    System.out.println("Time: " + (int)(System.currentTimeMillis() - startTime));
                    
                    try {
                        FileWriter fw = new FileWriter("dump");
                        for(int i = 0; i < mem.length; i++)
                            fw.write(mem[i] + "\n");
                        fw.close();
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                    
                    System.out.println("===============================");
                    
                    System.exit(0);
                } else if(currentPort == 0) Iiixmish2.vmem[1900] = e.getKeyChar();
            }
            public void keyReleased(KeyEvent e) {}
            public void keyTyped(KeyEvent e) {}
        });
        DISPLAY.fr.setVisible(true);
        DISPLAY.fr.repaint();
        
        startTime = System.currentTimeMillis();
        memExec();
    }
    static void memExec() {
        for(; pc < Iiixmish2.mem.length; pc++) {
            if(pc == 0) pc++;
            try {
                if(mem[pc] >= 0) continue;
                int a25 = mem[pc - 1] >> 6;
                int a6 = mem[pc - 1] & 0x3f;
                int b25 = (-mem[pc]) >> 6;
                int instr = (-mem[pc]) & 0x3f;
                
                if(instr == NOP)
                    Thread.sleep(1);
                /* ra = rb + rc */
                else if(instr == ADD) {
                    ureg[b25] = ureg[a25] + ureg[a6];
                }
                /* ra = rb - rc */
                else if(instr == SUB) {
                    ureg[b25] = ureg[a25] - ureg[a6];
                }
                /* ra = val */
                else if(instr == MOV) {
                    ureg[b25] = a25;
                }
                /* сохранение/загрузка */
                else if(instr == ISV) {
                    int addr = b25;
                    
                    if(pc > (mem[0]-1) && addr < mem[0])
                        continue; /* memory protection */
                    
                    Iiixmish2.mem[addr] = ureg[a6];
                }
                else if(instr == RISV) {
                    int addr = ureg[b25];
                    
                    if(pc > (mem[0]-1) && addr < mem[0])
                        continue; /* memory protection */
                    
                    Iiixmish2.mem[addr] = ureg[a6];
                }
                else if(instr == VSV) {
                    Iiixmish2.vmem[b25] = ureg[a6];
                    if(currentPort == 0) DISPLAY.exec();
                    else if(currentPort > 0) {
                        try {
                            FileWriter fw = new FileWriter(".fcomm" + currentPort);
                            String save = "";
                            for(int i = 0; i < vmem.length; i++)
                                save += "" + vmem[i] + "\n";
                            fw.write(save);
                            fw.close();
                        } catch(Exception ex) {}
                    }
                } else if(instr == VLD) {
                    if(currentPort > 0) {
                        try {
                            Scanner sc = new Scanner(new File(".comm" + currentPort));
                            for(int i = Integer.parseInt(sc.nextLine()); i < vmem.length && sc.hasNextLine(); i++)
                                vmem[i] = Integer.parseInt(sc.nextLine());
                            sc.close();
                        } catch(Exception ex) {}
                    }
                    ureg[a6] = Iiixmish2.vmem[b25];
                } else if(instr == RVSV) {
                    Iiixmish2.vmem[ureg[b25]] = ureg[a6];
                    if(currentPort == 0) DISPLAY.exec();
                    else if(currentPort > 0) {
                        try {
                            FileWriter fw = new FileWriter(".fcomm" + currentPort);
                            String save = "";
                            for(int i = 0; i < vmem.length; i++)
                                save += "" + vmem[i] + "\n";
                            fw.write(save);
                            fw.close();
                        } catch(Exception ex) {}
                    }
                } else if(instr == RVLD) {
                    if(currentPort > 0) {
                        try {
                            Scanner sc = new Scanner(new File(".comm" + currentPort));
                            for(int i = Integer.parseInt(sc.nextLine()); i < vmem.length && sc.hasNextLine(); i++)
                                vmem[i] = Integer.parseInt(sc.nextLine());
                            sc.close();
                        } catch(Exception ex) {}
                    }
                    ureg[a6] = Iiixmish2.vmem[ureg[b25]];
                } else if(instr == OPEN && pc < mem[0]) {
                    currentPort = b25;
                } else if(instr == ILD) {
                    int addr = b25;
                    
                    if(pc > (mem[0]-1) && addr < mem[0])
                        continue; /* memory protection */
                    
                    ureg[a6] = Iiixmish2.mem[addr];
                }
                else if(instr == RILD) {
                    int addr = ureg[b25];
                    
                    if(pc > (mem[0]-1) && addr < mem[0])
                        continue; /* memory protection */
                    
                    ureg[a6] = Iiixmish2.mem[addr];
                }
                /* условные переходы */
                else if(instr == IFA && ureg[b25] == ureg[a6]) {
                    if(pc > (mem[0]-1) && ureg[a25] < mem[0])
                        continue; /* code protection */
                    
                    pc = ureg[a25] - 1;
                } else if(instr == IFB && ureg[b25] != ureg[a6]) {
                    if(pc > (mem[0]-1) && ureg[a25] < mem[0])
                        continue; /* code protection */
                    
                    pc = ureg[a25] - 1;
                } else if(instr == IFC && ureg[b25] > ureg[a6]) {
                    if(pc > (mem[0]-1) && ureg[a25] < mem[0])
                        continue; /* code protection */
                    
                    pc = ureg[a25] - 1;
                } else if(instr == IFD && ureg[b25] < ureg[a6]) {
                    if(pc > (mem[0]-1) && ureg[a25] < mem[0])
                        continue; /* code protection */
                    
                    pc = ureg[a25] - 1;
                }
                /* OFF */
                else if(instr == OFF && pc < mem[0])
                    System.exit(0);
                else if(instr == OFF)
                    pc = 511;
                /* переход */
                else if(instr == JMP) {
                    if(pc > (mem[0]-1) && ureg[b25] < mem[0])
                        continue; /* code protection */
                    
                    pc = ureg[b25] - 1;
                }
                else if(instr == CALL) {
                    if(pc > (mem[0]-1) && b25 < mem[0])
                        continue; /* code protection */
                    
                    ureg[a6] = pc + 1;
                    pc = b25 - 1;
                }
                /* ещё команды */
                else if(instr == MUL) {
                    ureg[b25] = ureg[a25] * ureg[a6];
                }
                else if(instr == DIV) {
                    ureg[b25] = ureg[a25] / ureg[a6];
                }
                else if(instr == INC) {
                    ureg[b25]++;
                }
                else if(instr == DEC) {
                    ureg[b25]--;
                }
                else if(instr == TNP) {
                    ureg[b25] = -ureg[a25];
                }
                else if(instr == MOD) {
                    ureg[b25] = ureg[a25] % ureg[a6];
                }
                /* битовые операции */
                else if(instr == LSHIFT)
                    ureg[b25] = ureg[a25] << ureg[a6];
                else if(instr == RSHIFT)
                    ureg[b25] = ureg[a25] >>> ureg[a6];
                else if(instr == XOR)
                    ureg[b25] = ureg[a25] ^ ureg[a6];
                else if(instr == OR)
                    ureg[b25] = ureg[a25] | ureg[a6];
                else if(instr == AND)
                    ureg[b25] = ureg[a25] & ureg[a6];
                else if(instr == TIME)
                    ureg[b25] = (int)((System.currentTimeMillis() - startTime) / 10);
                else if(instr == TRST)
                    startTime = System.currentTimeMillis();
            } catch(Exception e) {
                System.err.println("iiixmish2: PC = " + pc + ",  " + e);
            }
        }
    }
}
class DISPLAY extends JPanel {
    /* панель и рамка */
    static DISPLAY p = new DISPLAY();
    static JFrame fr;
    
    public static void exec() {
        if(Iiixmish2.currentPort != 0) return;
        else if(Iiixmish2.vmem[1901] > 0) {
            fr.repaint();
            Iiixmish2.vmem[1901] = 0;
        }
        else if(Iiixmish2.vmem[1902] > 0) {
            for(int i = 0; i <= 1890; i++)
                Iiixmish2.vmem[i] = 0;
            Iiixmish2.vmem[1902] = 0;
        }
    }
    
    public void paint(java.awt.Graphics g) {
        if(Iiixmish2.currentPort != 0) return;
        /* выбрать цвет для фона */
        if(Iiixmish2.vmem[1998] == 1)      g.setColor(new Color(255, 255, 255));
        else if(Iiixmish2.vmem[1998] == 2) g.setColor(new Color(0, 255, 0));
        else if(Iiixmish2.vmem[1998] == 3) g.setColor(new Color(0, 0, 255));
        else if(Iiixmish2.vmem[1998] == 4) g.setColor(new Color(0, 120, 0));
        else if(Iiixmish2.vmem[1998] == 5) g.setColor(new Color(120, 120, 120));
        else if(Iiixmish2.vmem[1998] == 6) g.setColor(new Color(255, 0, 0));
        else if(Iiixmish2.vmem[1998] == 7) g.setColor(new Color(255, 255, 0));
        else g.setColor(new Color(0, 0, 0));
        /* рисовать прямоугольник (фон) */
        g.fillRect(0, 0, 572, 576);
        /* выбрать шрифт для ячеек видеопамяти */
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14));
        /* работа рисования ячеек видеопамяти */
        int i2 = 0;
        int i3 = 2;
        int i4 = 15;
        for(int i = 0; i < 30; i++) {
            for(int ii = 0; ii < 63; ii++) {
                /* выбрать цвет для ячеек видеопамяти */
                if(Iiixmish2.vmem[1999] == 1)      g.setColor(new Color(255, 255, 255));
                else if(Iiixmish2.vmem[1999] == 2) g.setColor(new Color(0, 255, 0));
                else if(Iiixmish2.vmem[1999] == 3) g.setColor(new Color(0, 0, 255));
                else if(Iiixmish2.vmem[1999] == 4) g.setColor(new Color(0, 120, 0));
                else if(Iiixmish2.vmem[1999] == 5) g.setColor(new Color(120, 120, 120));
                else if(Iiixmish2.vmem[1999] == 6) g.setColor(new Color(255, 0, 0));
                else if(Iiixmish2.vmem[1999] == 7) g.setColor(new Color(255, 255, 0));
                else g.setColor(new Color(0, 0, 0));
                
                if(Iiixmish2.vmem[(int)i2] < 191 && Iiixmish2.vmem[(int)i2] > 0 ||
                   Iiixmish2.vmem[(int)i2] > 1039 && Iiixmish2.vmem[(int)i2] < 1106 ||
                   Iiixmish2.vmem[(int)i2] > 9599 && Iiixmish2.vmem[(int)i2] < 9621 ||
                   Iiixmish2.vmem[(int)i2] == '—' || Iiixmish2.vmem[(int)i2] == 'Ё')
                    g.drawString("" + (char)Iiixmish2.vmem[i2], i3, i4);
                else if(Iiixmish2.vmem[i2] > 254 && Iiixmish2.vmem[i2] < 270)
                    Iiixmish2.vmem[1999] = (char)((int)Iiixmish2.vmem[i2] - 255);
                
                i2++;
                i3 += 9;
            }
            i3 = 2;
            i4 += 18;
        }
    }
}
