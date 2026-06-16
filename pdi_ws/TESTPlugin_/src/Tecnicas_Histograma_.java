import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Tecnicas_Histograma_ implements PlugIn {

    private ImagePlus imgAtual;
    private ImageProcessor procOriginal; 
    private ImageProcessor procModificado; 
    private final String[] METODOS = {"Expansão de Histograma", "Equalização de Histograma"};

    @Override
    public void run(String arg) {
        imgAtual = IJ.getImage();

        if (imgAtual == null) {
            IJ.error("Erro", "Você precisa ter uma imagem aberta.");
            return;
        }

        // As operações de histograma são para escala de cinza
        if (imgAtual.getType() != ImagePlus.GRAY8) {
            IJ.error("Erro", "A imagem selecionada precisa ser de 8-bits.");
            return;
        }

        procModificado = imgAtual.getProcessor();
        procOriginal = procModificado.duplicate();

        apresentarInterfaceGrafica();
    }

    private void apresentarInterfaceGrafica() {
        GenericDialog gd = new GenericDialog("Modificação de Histograma");
        
        // Adiciona os botões de rádio para a escolha da técnica
        gd.addRadioButtonGroup("Selecione a Estratégia:", METODOS, 2, 1, METODOS[0]);
        
        gd.showDialog();

        // Se o usuário clicar em cancelar, encerra o plugin sem alterar nada
        if (gd.wasCanceled()) {
            return;
        } 
        
        String metodoSelecionado = gd.getNextRadioButton();
        processarImagem(metodoSelecionado);
    }

    private void processarImagem(String metodo) {
        int largura = imgAtual.getWidth();
        int altura = imgAtual.getHeight();

        if (metodo.equals(METODOS[0])) {
            // ==========================================
            // 			EXPANSÃO DE HISTOGRAMA
            // ==========================================
            int aLow = 255;
            int aHigh = 0;

            // Encontrar o menor e maior valor de pixel presentes na imagem original
            for (int x = 0; x < largura; x++) {
                for (int y = 0; y < altura; y++) {
                    int val = procOriginal.getPixel(x, y);
                    if (val < aLow){
                         aLow = val;
                    }
                    if (val > aHigh){
                         aHigh = val;
                    }
                }
            }

            // Evitar divisão por zero caso a imagem tenha apenas um tom
            if (aHigh == aLow) {
                procModificado.insert(procOriginal, 0, 0); 
            } else {
                for (int x = 0; x < largura; x++) {
                    for (int y = 0; y < altura; y++) {
                        int a = procOriginal.getPixel(x, y);
                        // Fórmula: a_min + (a - a_low) * ((a_max - a_min) / (a_high - a_low))
                        // Onde a_min = 0 e a_max = 255
                        double novoPixel = (a - aLow) * (255.0 / (aHigh - aLow));
                        procModificado.putPixel(x, y, (int) Math.round(novoPixel));
                    }
                }
            }

        } else if (metodo.equals(METODOS[1])) {
            // ==========================================
            // 		EQUALIZAÇÃO DE HISTOGRAMA
            // ==========================================
        	int[] histograma = montarHistograma(procOriginal, largura, altura);
            int MN = largura * altura; // Número total de pixels
            
            int[] lut = new int[256]; // LUT para os novos valores
            double probabilidadeAcumulada = 0;

            // Calcula a probabilidade acumulada
            for (int i = 0; i < 256; i++) {
                probabilidadeAcumulada += (double) histograma[i] / MN;
                // Multiplicar o valor máximo (nesse caso 255) pela probabilidade acumulada e arredondar
                lut[i] = (int) Math.round(255.0 * probabilidadeAcumulada);
            }

            // Mapeia os pixels da imagem para os novos valores da paleta
            for (int x = 0; x < largura; x++) {
                for (int y = 0; y < altura; y++) {
                    int a = procOriginal.getPixel(x, y);
                    procModificado.putPixel(x, y, lut[a]);
                }
            }
        }

        // Atualiza a imagem na interface do ImageJ apenas no final
        imgAtual.updateAndDraw();
    }
    
    private int[] montarHistograma(ImageProcessor proc, int largura, int altura) {
        int[] hist = new int[256]; // No caso da escala de cinza, o histograma tem 256 intensidades
        
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                int intensidade = proc.getPixel(x, y);
                hist[intensidade]++; // incrementando o valor dentro do vetor no indice "intensidade"
            }
        }
        
        return hist;
    }
}