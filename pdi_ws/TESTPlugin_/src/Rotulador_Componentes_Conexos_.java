import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Rotulador_Componentes_Conexos_ implements PlugIn {

    @Override
    public void run(String arg) {
        // Obtém a imagem que está aberta e ativa no ImageJ no momento
        ImagePlus imagemAberta = IJ.getImage();
        
        if (imagemAberta == null) {
            IJ.error("Erro", "Nenhuma imagem está aberta para ser processada. Abra uma imagem binária primeiro.");
            return;
        }

        // O algoritmo trabalha diretamente na matriz de pixels, então pegamos o ImageProcessor
        ImageProcessor processadorDeImagem = imagemAberta.getProcessor();
        
        // ==========================================
        // PROCESSAMENTO PRINCIPAL
        // ==========================================
        
        // 1. Executa o algoritmo de rotulação de componentes conexos
        // Agora retorna apenas a matriz de inteiros
        int[][] matrizDeRotulos = aplicarAlgoritmoComponentesConexos(processadorDeImagem);

        // 2. Gera a imagem de saída pintando cada componente com um tom de cinza
        ImageProcessor imagemFinalProcessor = gerarImagemComTonsDeCinza(
            matrizDeRotulos, 
            processadorDeImagem.getWidth(), 
            processadorDeImagem.getHeight()
        );

        // 3. Exibe o resultado em uma nova janela para o usuário
        ImagePlus imagemFinal = new ImagePlus("Componentes Conexos Rotulados", imagemFinalProcessor);
        imagemFinal.show();
    }

    // ==========================================
    // ALGORITMO - COMPONENTES CONEXOS
    // ==========================================

    private int[][] aplicarAlgoritmoComponentesConexos(ImageProcessor processadorDeImagem) {
        int largura = processadorDeImagem.getWidth();
        int altura = processadorDeImagem.getHeight();

        // Imagem Rotulada J: Matriz para guardar os rótulos
        int[][] matrizDeRotulos = new int[largura][altura];
        
        // Auxiliar Fila FIFO para a busca de vizinhos
        Queue<Point> filaDePixels = new LinkedList<>();
        
        // Auxiliar: Variável inteira para o rótulo atual (label = 1)
        int rotuloAtual = 1;

        
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                
                int valorDoPixelOriginal = processadorDeImagem.getPixel(x, y);
                
                // Se I(p) != 0 (consideramos pixel > 0 como objeto) e J(p) == 0 (ainda não rotulado)
                if (valorDoPixelOriginal > 0 && matrizDeRotulos[x][y] == 0) {
                    
                    // J(p) = label
                    matrizDeRotulos[x][y] = rotuloAtual;
                    
                    // Insira p em Q
                    filaDePixels.add(new Point(x, y));

                    // Enquanto Q não for vazio
                    while (!filaDePixels.isEmpty()) {
                        
                        // Remova p de Q
                        Point pixelAnalisado = filaDePixels.poll();

                        // Para todo vizinho q na Vizinhança-4 (Cima, Baixo, Esquerda, Direita)
                        List<Point> vizinhos = obterVizinhosVizinhança4(pixelAnalisado.x, pixelAnalisado.y, largura, altura);
                        
                        for (Point vizinho : vizinhos) {
                            int valorDoVizinhoNaOriginal = processadorDeImagem.getPixel(vizinho.x, vizinho.y);
                            
                            // Se J(q) == 0 e I(p) == I(q)
                            if (matrizDeRotulos[vizinho.x][vizinho.y] == 0 && valorDoPixelOriginal == valorDoVizinhoNaOriginal) {
                                
                                // J(q) = J(p)
                                matrizDeRotulos[vizinho.x][vizinho.y] = matrizDeRotulos[pixelAnalisado.x][pixelAnalisado.y];
                                
                                // Insira q em Q
                                filaDePixels.add(vizinho);
                            }
                        }
                    }
                    
                    // label = label + 1
                    rotuloAtual = rotuloAtual + 1;
                }
            }
        }
        
        // Retornamos apenas a matriz
        return matrizDeRotulos;
    }

    // ==========================================
    // MÉTODOS VISUAIS E DE CONVERSÃO
    // ==========================================

    private ImageProcessor gerarImagemComTonsDeCinza(int[][] matrizDeRotulos, int largura, int altura) {
        ByteProcessor processadorDeSaida = new ByteProcessor(largura, altura);
        
        // Descobrindo a quantidade total de rótulos buscando o maior número na matriz
        int totalDeRotulos = 0;
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                if (matrizDeRotulos[x][y] > totalDeRotulos) {
                    totalDeRotulos = matrizDeRotulos[x][y];
                }
            }
        }
        
        // Evita divisão por zero caso a imagem seja toda preta
        if (totalDeRotulos == 0) {
            totalDeRotulos = 1; 
        }
        
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                int rotuloDestePixel = matrizDeRotulos[x][y];
                
                if (rotuloDestePixel > 0) {
                    // Mapeia o rótulo para um tom de cinza entre 1 e 255.
                    // Isso permite que cada componente receba uma cor única baseada no total de componentes.
                    int tomDeCinza = (int) (((double) rotuloDestePixel / totalDeRotulos) * 255);
                    processadorDeSaida.putPixel(x, y, tomDeCinza);
                } else {
                    // Pixels que permaneceram com rótulo 0 são o fundo, logo, são pintados de preto.
                    processadorDeSaida.putPixel(x, y, 0);
                }
            }
        }
        return processadorDeSaida;
    }

    // ==========================================
    // AUXILIARES
    // ==========================================

    private List<Point> obterVizinhosVizinhança4(int x, int y, int largura, int altura) {
        List<Point> listaDeVizinhos = new ArrayList<>();
        
        // Sempre verificamos as bordas da imagem antes de adicionar
        
        if (x > 0) {
            listaDeVizinhos.add(new Point(x - 1, y)); // Esquerda
        }
        
        if (x < largura - 1) {
            listaDeVizinhos.add(new Point(x + 1, y)); // Direita
        }
        
        if (y > 0) {
            listaDeVizinhos.add(new Point(x, y - 1)); // Cima
        }
        
        if (y < altura - 1) {
            listaDeVizinhos.add(new Point(x, y + 1)); // Baixo
        }
        
        return listaDeVizinhos;
    }
}