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
    static int[] ureg = new int[44];  // регистры
    static int pc = 0, ir = 0;
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
    ,IFA=    -11
    ,IFB=    -12
    ,IFC=    -13
    ,IFD=    -14
    ,UPDD=   -15
    ,OFF=    -16
    ,VRST=   -17
    ,JMP=    -18
    ,MUL=    -21
    ,DIV=    -22
    ,INC=    -26
    ,DEC=    -27
    ,TNP=    -28
    ,MOD=    -29
    ,LSHIFT= -38
    ,RSHIFT= -39
    ,XOR=    -40
    ,OR=     -41
    ,AND=    -42
    ,TIME=   -43
    ,TRST=   -44
    ,RISV=   -45
    ,RVSV=   -46
    ,RILD=   -47
    ,RVLD=   -48
    ;
    
    /* память
       ======
       код вне 0-65535 не может читать/прыгать/писать в 0-65535,
       переход к 32768 возможен с OFF;
       код вне 0-65535 не может читать 9999872-9999999;
       код вне 0-65535 не может писать в 9999000-9999099;
     */
    static int mem[] = new int[10000000];
    
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
        for(int i = 0; i < 2000; i++) {
            if(i < ureg.length) ureg[i] = 0;
            DISPLAY.vmem[i] = 0;
        }
        /* настройка экрана */
        DISPLAY.vmem[DISPLAY.vmem.length - 2] = 0; // параметр цвета для фона
        DISPLAY.vmem[DISPLAY.vmem.length - 1] = 1; // параметр цвета для ячеек видеопамяти
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
                } else ureg[1] = e.getKeyChar();
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
            try {
                ir = Iiixmish2.mem[pc];
                
                if(ir == NOP)
                    Thread.sleep(1);
                /* ra = rb + rc */
                else if(ir == ADD) {
                    ureg[Iiixmish2.mem[pc - 1]] = ureg[Iiixmish2.mem[pc - 2]] + ureg[Iiixmish2.mem[pc - 3]];
                }
                /* ra = rb - rc */
                else if(ir == SUB) {
                    ureg[Iiixmish2.mem[pc - 1]] = ureg[Iiixmish2.mem[pc - 2]] - ureg[Iiixmish2.mem[pc - 3]];
                }
                /* ra = val */
                else if(ir == MOV) {
                    ureg[Iiixmish2.mem[pc - 1]] = Iiixmish2.mem[pc - 2];
                }
                /* сохранение/загрузка */
                else if(ir == ISV) {
                    int addr = Iiixmish2.mem[pc - 1];
                    
                    if(pc > 65535 && (addr < 65536 || (addr >= 9999000 && addr < 9999100)))
                        continue; /* memory protection */
                    
                    Iiixmish2.mem[addr] = ureg[Iiixmish2.mem[pc - 2]];
                    
                    if(addr >= 9999000 && addr < 9999100) {
                        try {
                            FileWriter comm2 = new FileWriter(".comm2");
                            for(int i = 9999000; i < 9999100; i++)
                                comm2.write(mem[i] + "\n");
                            comm2.close();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                else if(ir == RISV) {
                    int addr = ureg[Iiixmish2.mem[pc - 1]];
                    
                    if(pc > 65535 && (addr < 65536 || (addr >= 9999000 && addr < 9999100)))
                        continue; /* memory protection */
                    
                    Iiixmish2.mem[addr] = ureg[Iiixmish2.mem[pc - 2]];
                    
                    if(addr >= 9999000 && addr < 9999100) {
                        try {
                            FileWriter comm2 = new FileWriter(".comm2");
                            for(int i = 9999000; i < 9999100; i++)
                                comm2.write(mem[i] + "\n");
                            comm2.close();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                else if(ir == VSV) {
                    int addr = Iiixmish2.mem[pc - 1];
                    
                    DISPLAY.vmem[addr] = (char)ureg[Iiixmish2.mem[pc - 2]];
                }
                else if(ir == RVSV) {
                    int addr = ureg[Iiixmish2.mem[pc - 1]];
                    
                    DISPLAY.vmem[addr] = (char)ureg[Iiixmish2.mem[pc - 2]];
                }
                else if(ir == LD) {
                    ureg[Iiixmish2.mem[pc - 1]] = ureg[Iiixmish2.mem[pc - 2]];
                }
                else if(ir == ILD) {
                    int addr = Iiixmish2.mem[pc - 2];
                    
                    if(pc > 65535 && (addr < 65536 || addr >= 9999872))
                        continue; /* memory protection */
                    
                    if(addr >= 9999872) {
                        try {
                            Scanner comm = new Scanner(new File(".comm"));
                            for(int i = 9999872; i < 10000000 && comm.hasNextLine(); i++)
                                mem[i] = Integer.parseInt(comm.nextLine());
                            comm.close();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    ureg[Iiixmish2.mem[pc - 1]] = Iiixmish2.mem[addr];
                }
                else if(ir == RILD) {
                    int addr = ureg[Iiixmish2.mem[pc - 2]];
                    
                    if(pc > 65535 && (addr < 65536 || addr >= 9999872))
                        continue; /* memory protection */
                    
                    if(addr >= 9999872) {
                        try {
                            Scanner comm = new Scanner(new File(".comm"));
                            for(int i = 9999872; i < 10000000 && comm.hasNextLine(); i++)
                                mem[i] = Integer.parseInt(comm.nextLine());
                            comm.close();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    ureg[Iiixmish2.mem[pc - 1]] = Iiixmish2.mem[addr];
                }
                else if(ir == VLD) {
                    int addr = Iiixmish2.mem[pc - 2];
                    
                    ureg[Iiixmish2.mem[pc - 1]] = DISPLAY.vmem[addr];
                }
                else if(ir == RVLD) {
                    int addr = ureg[Iiixmish2.mem[pc - 2]];
                    
                    ureg[Iiixmish2.mem[pc - 1]] = DISPLAY.vmem[addr];
                }
                /* условные переходы */
                else if(ir == IFA && ureg[Iiixmish2.mem[pc - 1]] == ureg[Iiixmish2.mem[pc - 2]]) {
                    if(pc > 65535 && ureg[Iiixmish2.mem[pc - 3]] < 65536)
                        continue; /* code protection */
                    
                    pc = ureg[Iiixmish2.mem[pc - 3]] - 1;
                } else if(ir == IFB && ureg[Iiixmish2.mem[pc - 1]] != ureg[Iiixmish2.mem[pc - 2]]) {
                    if(pc > 65535 && ureg[Iiixmish2.mem[pc - 3]] < 65536)
                        continue; /* code protection */
                    
                    pc = ureg[Iiixmish2.mem[pc - 3]] - 1;
                } else if(ir == IFC && ureg[Iiixmish2.mem[pc - 1]] > ureg[Iiixmish2.mem[pc - 2]]) {
                    if(pc > 65535 && ureg[Iiixmish2.mem[pc - 3]] < 65536)
                        continue; /* code protection */
                    
                    pc = ureg[Iiixmish2.mem[pc - 3]] - 1;
                } else if(ir == IFD && ureg[Iiixmish2.mem[pc - 1]] < ureg[Iiixmish2.mem[pc - 2]]) {
                    if(pc > 65535 && ureg[Iiixmish2.mem[pc - 3]] < 65536)
                        continue; /* code protection */
                    
                    pc = ureg[Iiixmish2.mem[pc - 3]] - 1;
                }
                /* обновление экрана */
                else if(ir == UPDD) {
                    DISPLAY.fr.repaint();
                }
                /* OFF */
                else if(ir == OFF && pc < 65536)
                    System.exit(0);
                else if(ir == OFF)
                    pc = 32767;
                /* частичный сброс видеопамяти */
                else if(ir == VRST) {
                    ureg[5] = 0;
                    for(int i = 0; i < 1890; i++) {
                        DISPLAY.vmem[i] = (char)ureg[5];
                    }
                }
                /* переход */
                else if(ir == JMP) {
                    if(pc > 65535 && ureg[Iiixmish2.mem[pc - 1]] < 65536)
                        continue; /* code protection */
                    
                    pc = ureg[Iiixmish2.mem[pc - 1]] - 1;
                }
                /* ещё команды */
                else if(ir == MUL) {
                    ureg[Iiixmish2.mem[pc - 1]] = ureg[Iiixmish2.mem[pc - 2]] * ureg[Iiixmish2.mem[pc - 3]];
                }
                else if(ir == DIV) {
                    ureg[Iiixmish2.mem[pc - 1]] = ureg[Iiixmish2.mem[pc - 2]] / ureg[Iiixmish2.mem[pc - 3]];
                }
                else if(ir == INC) {
                    ureg[Iiixmish2.mem[pc - 1]]++;
                }
                else if(ir == DEC) {
                    ureg[Iiixmish2.mem[pc - 1]]--;
                }
                else if(ir == TNP) {
                    ureg[Iiixmish2.mem[pc - 1]] = -ureg[Iiixmish2.mem[pc - 1]];
                }
                else if(ir == MOD) {
                    ureg[Iiixmish2.mem[pc - 1]] = ureg[Iiixmish2.mem[pc - 2]] % ureg[Iiixmish2.mem[pc - 3]];
                }
                /* битовые операции */
                else if(ir == LSHIFT)
                    ureg[Iiixmish2.mem[pc - 1]] = ureg[Iiixmish2.mem[pc - 2]] << ureg[Iiixmish2.mem[pc - 3]];
                else if(ir == RSHIFT)
                    ureg[Iiixmish2.mem[pc - 1]] = ureg[Iiixmish2.mem[pc - 2]] >> ureg[Iiixmish2.mem[pc - 3]];
                else if(ir == XOR)
                    ureg[Iiixmish2.mem[pc - 1]] = ureg[Iiixmish2.mem[pc - 2]] ^ ureg[Iiixmish2.mem[pc - 3]];
                else if(ir == OR)
                    ureg[Iiixmish2.mem[pc - 1]] = ureg[Iiixmish2.mem[pc - 2]] | ureg[Iiixmish2.mem[pc - 3]];
                else if(ir == AND)
                    ureg[Iiixmish2.mem[pc - 1]] = ureg[Iiixmish2.mem[pc - 2]] & ureg[Iiixmish2.mem[pc - 3]];
                else if(ir == TIME)
                    ureg[Iiixmish2.mem[pc - 1]] = (int)(System.currentTimeMillis() - startTime);
                else if(ir == TRST)
                    startTime = System.currentTimeMillis();
            } catch(Exception e) {
                System.err.println("iiixmish2: PC = " + pc + ",  " + e);
            }
        }
    }
}
class DISPLAY extends JPanel {
    /* видеопамять */
    static char vmem[] = new char[2000];
    /* панель и рамка */
    static DISPLAY p = new DISPLAY();
    static JFrame fr;
    
    public void paint(java.awt.Graphics g) {
        /* выбрать цвет для фона */
        if(DISPLAY.vmem[vmem.length - 2] == 1)      g.setColor(new Color(255, 255, 255));
        else if(DISPLAY.vmem[vmem.length - 2] == 2) g.setColor(new Color(0, 255, 0));
        else if(DISPLAY.vmem[vmem.length - 2] == 3) g.setColor(new Color(0, 0, 255));
        else if(DISPLAY.vmem[vmem.length - 2] == 4) g.setColor(new Color(0, 120, 0));
        else if(DISPLAY.vmem[vmem.length - 2] == 5) g.setColor(new Color(120, 120, 120));
        else if(DISPLAY.vmem[vmem.length - 2] == 6) g.setColor(new Color(255, 0, 0));
        else if(DISPLAY.vmem[vmem.length - 2] == 7) g.setColor(new Color(255, 255, 0));
        else g.setColor(new Color(0, 0, 0));
        /* рисовать прямоугольник (фон) */
        g.fillRect(0, 0, 572, 576);
        /* выбрать шрифт для ячеек видеопамяти */
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14));
        /* работа рисования ячеек видеопамяти */
        Iiixmish2.ureg[2] = 0;
        Iiixmish2.ureg[3] = 2;
        Iiixmish2.ureg[4] = 15;
        for(int i = 0; i < 30; i++) {
            for(int ii = 0; ii < 63; ii++) {
                /* выбрать цвет для ячеек видеопамяти */
                if(DISPLAY.vmem[vmem.length - 1] == 1)      g.setColor(new Color(255, 255, 255));
                else if(DISPLAY.vmem[vmem.length - 1] == 2) g.setColor(new Color(0, 255, 0));
                else if(DISPLAY.vmem[vmem.length - 1] == 3) g.setColor(new Color(0, 0, 255));
                else if(DISPLAY.vmem[vmem.length - 1] == 4) g.setColor(new Color(0, 120, 0));
                else if(DISPLAY.vmem[vmem.length - 1] == 5) g.setColor(new Color(120, 120, 120));
                else if(DISPLAY.vmem[vmem.length - 1] == 6) g.setColor(new Color(255, 0, 0));
                else if(DISPLAY.vmem[vmem.length - 1] == 7) g.setColor(new Color(255, 255, 0));
                else g.setColor(new Color(0, 0, 0));
                
                if(DISPLAY.vmem[(int)Iiixmish2.ureg[2]] < 191 && DISPLAY.vmem[(int)Iiixmish2.ureg[2]] > 0 ||
                   DISPLAY.vmem[(int)Iiixmish2.ureg[2]] > 1039 && DISPLAY.vmem[(int)Iiixmish2.ureg[2]] < 1106 ||
                   DISPLAY.vmem[(int)Iiixmish2.ureg[2]] > 9599 && DISPLAY.vmem[(int)Iiixmish2.ureg[2]] < 9621 ||
                   DISPLAY.vmem[(int)Iiixmish2.ureg[2]] == '—' || DISPLAY.vmem[(int)Iiixmish2.ureg[2]] == 'Ё')
                    g.drawString("" + DISPLAY.vmem[(int)Iiixmish2.ureg[2]], (int)Iiixmish2.ureg[3], (int)Iiixmish2.ureg[4]);
                else if(vmem[(int)Iiixmish2.ureg[2]] > 254 && vmem[(int)Iiixmish2.ureg[2]] < 270)
                    DISPLAY.vmem[vmem.length - 1] = (char)((int)vmem[(int)Iiixmish2.ureg[2]] - 255);
                
                Iiixmish2.ureg[2]++;
                Iiixmish2.ureg[3] += 9;
            }
            Iiixmish2.ureg[3] = 2;
            Iiixmish2.ureg[4] += 18;
        }
    }
    
}
