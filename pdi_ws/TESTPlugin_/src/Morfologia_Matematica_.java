import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Morfologia_Matematica_ implements PlugIn {

    private ImagePlus imgAtual;
    
    // Array com todas as operações descritas nos slides
    private final String[] METODOS = {
        "Dilatação: Expande contornos e fecha pequenos orifícios",
        "Erosão: Remove pequenos detalhes e afina os contornos",
        "Abertura: Suaviza contornos e rompe istmos (Erosão seguida de Dilatação)",
        "Fechamento: Funde descontinuidades e elimina buracos (Dilatação seguida de Erosão)",
        "Extração de Borda: Subtrai a imagem erodida da imagem original",
        "Esqueletização: Transforma o objeto em um esqueleto mais simples"
    };

    @Override
    public void run(String arg) {
        imgAtual = IJ.getImage();

        if (imgAtual == null || imgAtual.getType() != ImagePlus.GRAY8) {
            IJ.error("Erro", "Abra uma imagem binária de 8-bits.");
            return;
        }

        apresentarInterfaceGrafica();
    }

    private void apresentarInterfaceGrafica() {
        GenericDialog gd = new GenericDialog("Morfologia Binária (Cruz 3x3)");
        
        gd.addMessage("Selecione a operação morfológica a ser aplicada.\n" +
                      "Nota: O algoritmo assume fundo preto (0) e objeto branco (255).");
        
        // Agora temos 6 opções no RadioButtonGroup
        gd.addRadioButtonGroup("Operações:", METODOS, 6, 1, METODOS[0]);
        
        gd.showDialog();

        if (gd.wasCanceled()) return;

        String metodoSelecionado = gd.getNextRadioButton();
        ImageProcessor procOriginal = imgAtual.getProcessor();

        // Direciona para o método correspondente baseado na seleção
        if (metodoSelecionado.equals(METODOS[0])) {
            ImageProcessor dilacao = dilataCruz(procOriginal);
            new ImagePlus("Dilatação", dilacao).show();
            
        } else if (metodoSelecionado.equals(METODOS[1])) {
            ImageProcessor erosao = erodeCruz(procOriginal);
            new ImagePlus("Erosão", erosao).show();
            
        } else if (metodoSelecionado.equals(METODOS[2])) {
            aplicarAbertura(procOriginal);
            
        } else if (metodoSelecionado.equals(METODOS[3])) {
            aplicarFechamento(procOriginal);
            
        } else if (metodoSelecionado.equals(METODOS[4])) {
            aplicarExtracaoBorda(procOriginal);
            
        } else {
            aplicarEsqueletizacao(procOriginal);
        }
    }

    // ==========================================
    // MÉTODOS DE APLICAÇÃO (COMPOSTOS)
    // ==========================================

    private void aplicarAbertura(ImageProcessor original) {
        // Abertura é Erosão seguida de Dilatação (Slide 15)
        ImageProcessor erosao = erodeCruz(original);
        ImageProcessor abertura = dilataCruz(erosao);
        new ImagePlus("Abertura", abertura).show();
    }

    private void aplicarFechamento(ImageProcessor original) {
        // Fechamento é Dilatação seguida de Erosão (Slide 16)
        ImageProcessor dilatacao = dilataCruz(original);
        ImageProcessor fechamento = erodeCruz(dilatacao);
        new ImagePlus("Fechamento", fechamento).show();
    }

    private void aplicarExtracaoBorda(ImageProcessor original) {
        // Borda é Original subtraído de sua Erosão (Slide 18)
        ImageProcessor erosao = erodeCruz(original);
        ImageProcessor borda = subtrai(original, erosao);
        new ImagePlus("Extração de Borda", borda).show();
    }

    private void aplicarEsqueletizacao(ImageProcessor original) {
        int largura = original.getWidth();
        int altura = original.getHeight();
        
        ImageProcessor esqueletoFinal = original.createProcessor(largura, altura);
        ImageProcessor Xk = original.duplicate();
        
        int limiteIteracoes = 0; 
        
        while (!isVazio(Xk) && limiteIteracoes < 1000) {
            ImageProcessor Xk_mais_1 = erodeCruz(Xk);
            ImageProcessor aberturaXk = dilataCruz(Xk_mais_1); // Abertura simplificada
            ImageProcessor Sk = subtrai(Xk, aberturaXk);
            
            esqueletoFinal = une(esqueletoFinal, Sk);
            Xk = Xk_mais_1;
            
            limiteIteracoes++;
        }
        
        new ImagePlus("Esqueleto Final (" + limiteIteracoes + " iteracoes)", esqueletoFinal).show();
    }


    // ==========================================
    // OPERAÇÕES MORFOLÓGICAS BÁSICAS (Elemento: Cruz 3x3)
    // ==========================================
    
    private ImageProcessor erodeCruz(ImageProcessor ip) {
        int largura = ip.getWidth();
        int altura = ip.getHeight();
        ImageProcessor resultado = ip.createProcessor(largura, altura);
        
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                boolean todosBrancos = (getPixel(ip, x, y) == 255)   &&
                                       (getPixel(ip, x - 1, y) == 255) &&
                                       (getPixel(ip, x + 1, y) == 255) &&
                                       (getPixel(ip, x, y - 1) == 255) &&
                                       (getPixel(ip, x, y + 1) == 255);
                
                if (todosBrancos) resultado.putPixel(x, y, 255);
                else resultado.putPixel(x, y, 0);
            }
        }
        return resultado;
    }

    private ImageProcessor dilataCruz(ImageProcessor ip) {
        int largura = ip.getWidth();
        int altura = ip.getHeight();
        ImageProcessor resultado = ip.createProcessor(largura, altura);
        
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                boolean algumBranco = (getPixel(ip, x, y) == 255)   ||
                                      (getPixel(ip, x - 1, y) == 255) ||
                                      (getPixel(ip, x + 1, y) == 255) ||
                                      (getPixel(ip, x, y - 1) == 255) ||
                                      (getPixel(ip, x, y + 1) == 255);
                
                if (algumBranco) resultado.putPixel(x, y, 255);
                else resultado.putPixel(x, y, 0);
            }
        }
        return resultado;
    }

    private ImageProcessor subtrai(ImageProcessor imgA, ImageProcessor imgB) {
        int largura = imgA.getWidth();
        int altura = imgA.getHeight();
        ImageProcessor resultado = imgA.createProcessor(largura, altura);
        
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                if (imgA.getPixel(x, y) == 255 && imgB.getPixel(x, y) == 0) {
                    resultado.putPixel(x, y, 255);
                } else {
                    resultado.putPixel(x, y, 0);
                }
            }
        }
        return resultado;
    }

    private ImageProcessor une(ImageProcessor imgA, ImageProcessor imgB) {
        int largura = imgA.getWidth();
        int altura = imgA.getHeight();
        ImageProcessor resultado = imgA.createProcessor(largura, altura);
        
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                if (imgA.getPixel(x, y) == 255 || imgB.getPixel(x, y) == 255) {
                    resultado.putPixel(x, y, 255);
                } else {
                    resultado.putPixel(x, y, 0);
                }
            }
        }
        return resultado;
    }

    private boolean isVazio(ImageProcessor ip) {
        for (int x = 0; x < ip.getWidth(); x++) {
            for (int y = 0; y < ip.getHeight(); y++) {
                if (ip.getPixel(x, y) == 255) return false;
            }
        }
        return true;
    }

    private int getPixel(ImageProcessor ip, int x, int y) {
        if (x < 0 || x >= ip.getWidth() || y < 0 || y >= ip.getHeight()) {
            return 0; 
        }
        return ip.getPixel(x, y);
    }
}