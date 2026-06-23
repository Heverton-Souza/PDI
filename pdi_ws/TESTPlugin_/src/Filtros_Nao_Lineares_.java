import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.util.Arrays;

public class Filtros_Nao_Lineares_ implements PlugIn {

    private ImagePlus imgAtual;
    private final String[] METODOS = {"Filtro Sobel (Detecção de Bordas)", "Filtro de Mediana"};

    @Override
    public void run(String arg) {
        imgAtual = IJ.getImage();

        if (imgAtual == null || imgAtual.getType() != ImagePlus.GRAY8) {
            IJ.error("Erro", "Abra uma imagem de 8-bits (tons de cinza).");
            return;
        }

        apresentarInterfaceGrafica();
    }

    private void apresentarInterfaceGrafica() {
        GenericDialog gd = new GenericDialog("Filtros Não Lineares");
        gd.addRadioButtonGroup("Selecione o Filtro:", METODOS, 2, 1, METODOS[0]);
        gd.showDialog();

        if (gd.wasCanceled()) return;

        String metodoSelecionado = gd.getNextRadioButton();
        ImageProcessor procOriginal = imgAtual.getProcessor();

        if (metodoSelecionado.equals(METODOS[0])) {
            aplicarSobel(procOriginal);
        } else {
            aplicarMediana(procOriginal);
        }
    }

    private void aplicarSobel(ImageProcessor original) {
        int largura = original.getWidth();
        int altura = original.getHeight();

        ImageProcessor procSobelV = original.duplicate();
        ImageProcessor procSobelH = original.duplicate();
        ImageProcessor procSobelFinal = original.duplicate();

        // Máscaras de Sobel
        int[][] sobelV = {{-1, 0, 1}, 
        				  {-2, 0, 2}, 
        				  {-1, 0, 1}};
        
        int[][] sobelH = {{ 1, 2, 1}, 
        				  { 0, 0, 0}, 
        				  {-1,-2,-1}};

        // Ignorando as bordas extremas
        // O for de fora está sempre apontando para o "pixel alvo"
        for (int x = 1; x < largura - 1; x++) {
            for (int y = 1; y < altura - 1; y++) {
                
                double pSobelVer = 0;
                double pSobelHor = 0;

             	// Percorrendo a vizinhança - Como o for de fora aponta para o alvo, o de dentro tem que pegar os pixels em volta do alvo
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int pixel = original.getPixel(x + i, y + j); 
                        pSobelVer += pixel * sobelV[i + 1][j + 1];
                        pSobelHor += pixel * sobelH[i + 1][j + 1];
                    }
                }

                // Cálculo da magnitude final
                double pSobelFinal = Math.sqrt(Math.pow(pSobelVer, 2) + Math.pow(pSobelHor, 2));

                // Restringindo aos limites de 8-bits
                procSobelV.putPixel(x, y, limitar(Math.abs(pSobelVer)));
                procSobelH.putPixel(x, y, limitar(Math.abs(pSobelHor)));
                procSobelFinal.putPixel(x, y, limitar(pSobelFinal));
            }
        }

        // Criando e exibindo as três novas imagens
        new ImagePlus("Sobel Vertical", procSobelV).show();
        new ImagePlus("Sobel Horizontal", procSobelH).show();
        new ImagePlus("Sobel Junção Final", procSobelFinal).show();
    }

    // Pega 3x3 pixels, ordena, pega a mediana do vetor e coloca no pixel novo
    private void aplicarMediana(ImageProcessor original) {
        int largura = original.getWidth();
        int altura = original.getHeight();
        ImageProcessor procModificado = original.duplicate();

        int[] vizinhos = new int[9]; // 3x3 pegando os vizinhos + o alvo

        // Ignorando as bordas extremas
        // O for de fora está sempre apontando para o "pixel alvo"
        for (int x = 1; x < largura - 1; x++) {
            for (int y = 1; y < altura - 1; y++) {
                
                int index = 0;
                
                // Percorrendo a vizinhança - Como o for de fora aponta para o alvo, o de dentro tem que pegar os pixels em volta do alvo
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        vizinhos[index] = original.getPixel(x + i, y + j);
                        index++;
                    }
                }
                
                // Ordena os valores
                Arrays.sort(vizinhos);
                
                // O valor mediano (nesse caso) é o índice 4 (o do meio no array de 0 a 8)
                procModificado.putPixel(x, y, vizinhos[4]);
            }
        }

        imgAtual.setProcessor(procModificado);
        imgAtual.updateAndDraw();
    }

    // Método auxiliar para evitar estouro de limite de cor
    private int limitar(double valor) {
        if (valor > 255) return 255;
        if (valor < 0) return 0;
        return (int) valor;
    }
}