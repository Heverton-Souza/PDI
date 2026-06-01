import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.awt.AWTEvent;

public class Operacoes_Ponto_A_Ponto_ implements PlugIn, DialogListener {

    private ImagePlus imgAtual;
    private ImageProcessor procOriginal; // Guarda os dados intactos
    private ImageProcessor procModificado; // Mostra na tela

    @Override
    public void run(String arg) {
        imgAtual = IJ.getImage();

        if (imgAtual == null) {
            IJ.error("Erro", "Você precisa ter uma imagem aberta.");
            return;
        }

        if (imgAtual.getType() != ImagePlus.COLOR_RGB) {
            IJ.error("Erro", "A imagem selecionada precisa ser RGB.");
            return;
        }

        // Duplica o processador para guardar os pixels originais como "gabarito"
        procModificado = imgAtual.getProcessor();
        procOriginal = procModificado.duplicate();

        apresentarInterfaceGrafica();
    }

    private void apresentarInterfaceGrafica() {
        GenericDialog gd = new GenericDialog("Operações Ponto a Ponto");
        
        // Adiciona o listener para atualizar a imagem em tempo real
        gd.addDialogListener(this);

        // Sliders (Nome, Mínimo, Máximo, Valor Padrão)
        gd.addSlider("Dessaturação (%)", 0, 100, 100);
        gd.addSlider("Brilho", -255, 255, 0);
        gd.addSlider("Contraste", -255, 255, 0);
        gd.addSlider("Limiar de Solarização", 0, 255, 255); // 255 significa que não inverte ninguém

        gd.showDialog();

        if (gd.wasCanceled()) {
            // Restaura a imagem original se o usuário cancelar
            imgAtual.setProcessor(procOriginal);
            imgAtual.updateAndDraw();
            IJ.showStatus("Ação cancelada. Imagem restaurada.");
        } else if (gd.wasOKed()) {
            IJ.showStatus("Operações Ponto a Ponto aplicadas com sucesso!");
        }
    }

    /**
     * Esse método roda toda vez que QUALQUER slider for movimentado pelo usuário.
     */
    @Override
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        if (gd.wasCanceled()) return false;

        // Pega os valores atuais dos sliders
        double sCol = gd.getNextNumber() / 100.0; // Divide por 100 para ficar entre 0 e 1
        double brilho = gd.getNextNumber();
        double contraste = gd.getNextNumber();
        double limiarSolarizacao = gd.getNextNumber();

        // Chama a função pesada de processamento
        processarImagem(sCol, brilho, contraste, limiarSolarizacao);
        
        return true;
    }

    /**
     * Varre a imagem, aplica as fórmulas matemáticas da literatura 
     * em cadeia e atualiza a tela.
     */
    private void processarImagem(double sCol, double brilho, double contraste, double limiarSolar) {
        int largura = imgAtual.getWidth();
        int altura = imgAtual.getHeight();

        // Fórmula do Fator de Contraste calculada fora do laço para otimização
        double F = (259.0 * (contraste + 255.0)) / (255.0 * (259.0 - contraste));

        int[] rgb = new int[3];

        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                
                // 1. LÊ SEMPRE DO PROCESSADOR ORIGINAL (Limpo)
                rgb = procOriginal.getPixel(x, y, rgb);
                double r = rgb[0];
                double g = rgb[1];
                double b = rgb[2];

                // ==========================================
                // 1º PASSO: DESSATURAÇÃO
                // ==========================================
                // Verifica se o usuário alterou o fator de saturação para não gastar processamento à toa
                if (sCol < 1.0) {
                    // Y da técnica Luminance
                    double yLuma = 0.299 * r + 0.587 * g + 0.114 * b;
                    
                    r = yLuma + sCol * (r - yLuma);
                    g = yLuma + sCol * (g - yLuma);
                    b = yLuma + sCol * (b - yLuma);
                }

                // ==========================================
                // 2º PASSO: BRILHO
                // ==========================================
                r = r + brilho;
                g = g + brilho;
                b = b + brilho;

                // ==========================================
                // 3º PASSO: CONTRASTE
                // ==========================================
                if (contraste != 0) {
                    r = F * (r - 128.0) + 128.0;
                    g = F * (g - 128.0) + 128.0;
                    b = F * (b - 128.0) + 128.0;
                }

                // -> Truncar os valores antes da solarização para evitar erros matemáticos
                r = truncar(r);
                g = truncar(g);
                b = truncar(b);

                // ==========================================
                // 4º PASSO: SOLARIZAÇÃO
                // ==========================================
                // Se o valor do pixel for MAIOR que o limiar definido, aplica o negativo nele
                if (r > limiarSolar) r = 255.0 - r;
                if (g > limiarSolar) g = 255.0 - g;
                if (b > limiarSolar) b = 255.0 - b;

                // -> Truncar novamente por segurança e converter para inteiro
                rgb[0] = (int) Math.round(truncar(r));
                rgb[1] = (int) Math.round(truncar(g));
                rgb[2] = (int) Math.round(truncar(b));

                // 2. SALVA O RESULTADO NO PROCESSADOR MODIFICADO (Que está na tela)
                procModificado.putPixel(x, y, rgb);
            }
        }

        // Avisa o ImageJ para atualizar a tela agora que todos os pixels foram recalculados
        imgAtual.updateAndDraw();
    }

    /**
     * Função auxiliar exigida pela literatura para garantir que o nível 
     * de cinza ou cor nunca ultrapasse os limites suportados de 8-bits (0 a 255).
     */
    private double truncar(double valor) {
        if (valor > 255.0) return 255.0;
        if (valor < 0.0) return 0.0;
        return valor;
    }
}