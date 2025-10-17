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
    static int[] ureg = new int[24]; // регистры
    static int pc = 1;
    public static final byte
     VSV=    1
    ,ADD=    2
    ,SUB=    3
    ,MOV=    4
    ,ILD=    5
    ,OPEN=   6
    ,ISV=    7
    ,CALL=   8
    ,RVLD=   9
    ,IFA=    10
    ,IFB=    11
    ,THEN=   12
    ,OFF=    13
    ,JMP=    14
    ,MUL=    15
    ,DIV=    16
    ,INC=    17
    ,DEC=    18
    ,TNP=    19
    ,REM=    20
    ,LSHIFT= 21
    ,RSHIFT= 22
    ,XOR=    23
    ,OR=     24
    ,AND=    25
    ,TIME=   26
    ,VLD=    27
    ,RISV=   28
    ,RILD=   29
    ,RVSV=   30
    ,NOP=    31
    ;
    
    /* память
       ======
       код вне 0..(unpriv-1) не может читать/прыгать/писать в 0..(unpriv-1),
       переход к syscall возможен с OFF;
       код вне 0..(unpriv-1) не может использовать инструкцию OPEN;
     */
    static int mem[] = new int[10000000];
    
    /* видеопамять */
    static int vmem[] = new int[2000];
    
    static int currentPort = -1, unpriv = 0, syscall = 0, interrupt = 0, offPC = 0;
    static byte flag = 0, savedflag = 0;
    
    private static long startTime;
    
    private static int virt(int addr) {
        if(pc < unpriv) return 0;
        int ptr = 3;
        if(mem[ptr+1] > 0 && addr >= mem[ptr] && addr < mem[ptr]+mem[ptr+1]) return mem[ptr+2]-mem[ptr]; ptr += 3;
        if(mem[ptr+1] > 0 && addr >= mem[ptr] && addr < mem[ptr]+mem[ptr+1]) return mem[ptr+2]-mem[ptr]; ptr += 3;
        if(mem[ptr+1] > 0 && addr >= mem[ptr] && addr < mem[ptr]+mem[ptr+1]) return mem[ptr+2]-mem[ptr]; ptr += 3;
        if(mem[ptr+1] > 0 && addr >= mem[ptr] && addr < mem[ptr]+mem[ptr+1]) return mem[ptr+2]-mem[ptr]; ptr += 3;
        if(mem[ptr+1] > 0 && addr >= mem[ptr] && addr < mem[ptr]+mem[ptr+1]) return mem[ptr+2]-mem[ptr]; ptr += 3;
        if(mem[ptr+1] > 0 && addr >= mem[ptr] && addr < mem[ptr]+mem[ptr+1]) return mem[ptr+2]-mem[ptr]; ptr += 3;
        if(mem[ptr+1] > 0 && addr >= mem[ptr] && addr < mem[ptr]+mem[ptr+1]) return mem[ptr+2]-mem[ptr]; ptr += 3;
        if(mem[ptr+1] > 0 && addr >= mem[ptr] && addr < mem[ptr]+mem[ptr+1]) return mem[ptr+2]-mem[ptr]; ptr += 3;
        return -addr - 1;
    }
    
    public static void main(String args[]) {
        if(args.length == 0) {
            System.out.println("Использование:  java downadow.iiixmish2.main.Iiixmish2 ПУТЬ_К_ОБРАЗУ");
            System.exit(0);
        }
        try {
            byte[] program = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(args[0]));
            for(int i = 0; i < program.length; i += 4) {
                mem[i / 4] = (Byte.toUnsignedInt(program[i + 0]) << 24) | (Byte.toUnsignedInt(program[i + 1]) << 16) |
                    (Byte.toUnsignedInt(program[i + 2]) << 8) | (Byte.toUnsignedInt(program[i + 3]));
            }
        } catch(Exception e) {}
        unpriv    = mem[0];
        interrupt = (mem[1] < unpriv ? mem[1] : unpriv);
        syscall   = (mem[2] < unpriv ? mem[2] : unpriv);
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
                    System.out.println("Time: " + (int)((System.currentTimeMillis() - startTime) / 10));
                    
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
        int ticks = 0;
        for(; pc >= 0; pc++) {
            try {
                if(pc >= mem.length) pc = mem.length - 1;
                if(pc >= unpriv) ticks++;
                if(pc >= unpriv && ticks >= 1000) {
                    offPC = pc;
                    savedflag = flag;
                    pc = interrupt;
                    ticks = 0;
                }
                if(pc < 27 || mem[pc] >= 0) continue;
                int a = mem[pc - 1] & 0x7fffffff;
                int b = (-mem[pc]) >> 11;
                int c = ((-mem[pc]) >> 6) & 0x1f;
                int d = ((-mem[pc]) >> 5) & 0x1;
                int instr = (-mem[pc]) & 0x1f;
                
                /* ra = rb + rc */
                if(instr == ADD) {
                    long res = (d == 0 ? ((long)ureg[a] + (long)ureg[b]) : (Integer.toUnsignedLong(ureg[a]) + Integer.toUnsignedLong(ureg[b])));
                    ureg[c] = (int)(res & 0xffffffffL);
                    flag = (byte)((d == 0 ? (long)ureg[c] : Integer.toUnsignedLong(ureg[c])) != res ? 1 : 0);
                }
                /* ra = rb - rc */
                else if(instr == SUB) {
                    long res = (d == 0 ? ((long)ureg[a] - (long)ureg[b]) : (Integer.toUnsignedLong(ureg[a]) - Integer.toUnsignedLong(ureg[b])));
                    ureg[c] = (int)(res & 0xffffffffL);
                    flag = (byte)((d == 0 ? (long)ureg[c] : Integer.toUnsignedLong(ureg[c])) != res ? 1 : 0);
                }
                /* ra = val */
                else if(instr == MOV) {
                    ureg[c] = a;
                }
                /* сохранение/загрузка */
                else if(instr == ISV && d == 0) {
                    int addr = a + virt(a);
                    
                    if(pc >= unpriv && addr < unpriv)
                        continue; /* memory protection */
                    
                    Iiixmish2.mem[addr] = ureg[b];
                }
                else if(instr == ISV) {
                    int addr = a / 4 + virt(a / 4);
                    
                    if(pc >= unpriv && addr < unpriv)
                        continue; /* memory protection */
                    
                    Iiixmish2.mem[addr] = (Iiixmish2.mem[addr] & ~(0xff << (24 - a % 4 * 8))) | ((ureg[b] & 0xff) << (24 - a % 4 * 8));
                }
                else if(instr == RISV && d == 0) {
                    int addr = ureg[a] + virt(ureg[a]);
                    
                    if(pc >= unpriv && addr < unpriv)
                        continue; /* memory protection */
                    
                    Iiixmish2.mem[addr] = ureg[b];
                    ureg[a] += c;
                }
                else if(instr == RISV) {
                    int addr = ureg[a] / 4 + virt(ureg[a] / 4);
                    
                    if(pc >= unpriv && addr < unpriv)
                        continue; /* memory protection */
                    
                    Iiixmish2.mem[addr] = (Iiixmish2.mem[addr] & ~(0xff << (24 - ureg[a] % 4 * 8))) | ((ureg[b] & 0xff) << (24 - ureg[a] % 4 * 8));
                    ureg[a] += c;
                }
                else if(instr == VSV) {
                    Iiixmish2.vmem[a] = ureg[b];
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
                            try {
                                for(int i = Integer.parseInt(sc.nextLine()); i < vmem.length && sc.hasNextLine(); i++)
                                    vmem[i] = Integer.parseInt(sc.nextLine());
                            } catch(Exception e) {}
                            sc.close();
                        } catch(Exception ex) {}
                    }
                    ureg[b] = Iiixmish2.vmem[a];
                } else if(instr == RVSV) {
                    Iiixmish2.vmem[ureg[a]] = ureg[b];
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
                            try {
                                for(int i = Integer.parseInt(sc.nextLine()); i < vmem.length && sc.hasNextLine(); i++)
                                    vmem[i] = Integer.parseInt(sc.nextLine());
                            } catch(Exception e) {}
                            sc.close();
                        } catch(Exception ex) {}
                    }
                    ureg[b] = Iiixmish2.vmem[ureg[a]];
                } else if(instr == OPEN && pc < unpriv) {
                    currentPort = b;
                } else if(instr == ILD && d == 0) {
                    int addr = a + virt(a);
                    
                    if(pc >= unpriv && addr < unpriv)
                        continue; /* memory protection */
                    
                    ureg[b] = Iiixmish2.mem[addr];
                } else if(instr == ILD) {
                    int addr = a / 4 + virt(a / 4);
                    
                    if(pc >= unpriv && addr < unpriv)
                        continue; /* memory protection */
                    
                    ureg[b] = (Iiixmish2.mem[addr] >> (24 - a % 4 * 8)) & 0xff;
                }
                else if(instr == RILD && d == 0) {
                    ureg[a] -= c;
                    int addr = ureg[a] + virt(ureg[a]);
                    
                    if(pc >= unpriv && addr < unpriv)
                        continue; /* memory protection */
                    
                    ureg[b] = Iiixmish2.mem[addr];
                } else if(instr == RILD) {
                    ureg[a] -= c;
                    int addr = ureg[a] / 4 + virt(ureg[a] / 4);
                    
                    if(pc >= unpriv && addr < unpriv)
                        continue; /* memory protection */
                    
                    ureg[b] = (Iiixmish2.mem[addr] >> (24 - ureg[a] % 4 * 8)) & 0xff;
                }
                /* условия */
                else if(instr == IFA) {
                    flag = (byte)(ureg[c] == ureg[b] ? 1 : 0);
                } else if(instr == IFB) {
                    flag = (byte)((d == 0 ? (ureg[c] < ureg[b]) : (Integer.toUnsignedLong(ureg[c]) < Integer.toUnsignedLong(ureg[b]))) ? 1 : 0);
                }
                /* OFF */
                else if(instr == OFF && pc >= unpriv) {
                    offPC = pc + 1;
                    savedflag = flag;
                    pc = syscall - 1;
                } else if(instr == OFF) {
                    ureg[b] = savedflag;
                    ureg[c] = offPC;
                }
                /* переход */
                else if(instr == JMP || (instr == THEN && flag != 0)) {
                    if(pc >= unpriv && (ureg[c] + virt(ureg[c])) < unpriv)
                        continue; /* code protection */
                    
                    flag = (byte)(ureg[b] != 0 ? 1 : 0);
                    pc = ureg[c] + virt(ureg[c]) - 1;
                }
                else if(instr == CALL) {
                    if(pc >= unpriv && (a + virt(a)) < unpriv)
                        continue; /* code protection */
                    
                    ureg[b] = pc - virt(pc) + 1;
                    pc = a + virt(a) - 1;
                }
                /* ещё команды */
                else if(instr == MUL) {
                    long res = (d == 0 ? ((long)ureg[a] * (long)ureg[b]) : (Integer.toUnsignedLong(ureg[a]) * Integer.toUnsignedLong(ureg[b])));
                    ureg[c] = (int)(res & 0xffffffffL);
                    flag = (byte)((d == 0 ? (long)ureg[c] : Integer.toUnsignedLong(ureg[c])) != res ? 1 : 0);
                }
                else if(instr == DIV && (int)(flag = (byte)((ureg[b] == 0 || (d == 0 && ureg[a] == Integer.MIN_VALUE && ureg[b] == -1)) ? 1 : 0)) == 0) {
                    ureg[c] = (d == 0 ? (ureg[a] / ureg[b]) : (int)(Integer.toUnsignedLong(ureg[a]) / Integer.toUnsignedLong(ureg[b])));
                }
                else if(instr == INC) {
                    ureg[c]++;
                }
                else if(instr == DEC) {
                    ureg[c]--;
                }
                else if(instr == TNP && (int)(flag = (byte)(ureg[b] == Integer.MIN_VALUE ? 1 : 0)) == 0) {
                    ureg[c] = -ureg[b];
                }
                else if(instr == REM && (int)(flag = (byte)((ureg[b] == 0 || (d == 0 && ureg[a] == Integer.MIN_VALUE && ureg[b] == -1)) ? 1 : 0)) == 0) {
                    ureg[c] = (d == 0 ? (ureg[a] % ureg[b]) : (int)(Integer.toUnsignedLong(ureg[a]) % Integer.toUnsignedLong(ureg[b])));
                }
                /* NOP */
                else if(instr == NOP) {
                    Thread.sleep(2);
                    if(pc >= unpriv) ticks += 500;
                }
                /* битовые операции */
                else if(instr == LSHIFT) {
                    long res = (Integer.toUnsignedLong(ureg[a]) << ureg[b]);
                    ureg[c] = (int)(res & 0xffffffffL);
                    flag = (byte)(Integer.toUnsignedLong(ureg[c]) != res ? 1 : 0);
                } else if(instr == RSHIFT)
                    ureg[c] = (ureg[a] >>> ureg[b]);
                else if(instr == XOR)
                    ureg[c] = (ureg[a] ^ ureg[b]);
                else if(instr == OR)
                    ureg[c] = (ureg[a] | ureg[b]);
                else if(instr == AND)
                    ureg[c] = (ureg[a] & ureg[b]);
                else if(instr == TIME)
                    ureg[c] = (int)((System.currentTimeMillis() - startTime) / 10);
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
